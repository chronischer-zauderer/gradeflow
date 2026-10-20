package com.autograder.sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Compiles student Python in a throwaway Docker container: no network, non-root user,
 * read-only mount of the work dir, time limit, captured output. Uses the {@code compile()}
 * builtin (equivalent compile check to {@code python -m py_compile}) so nothing is written
 * into the mounted dir.
 */
@Component
public class DockerSandboxExecutor implements SandboxExecutor {

    private static final String CHECK = "compile(open('Main.py','rb').read(), 'Main.py', 'exec')";

    private final SandboxProperties props;

    public DockerSandboxExecutor(SandboxProperties props) {
        this.props = props;
    }

    @Override
    public SandboxResult compile(String sourceCode) {
        Path workDir;
        try {
            workDir = Files.createTempDirectory("gradeflow-sbx-");
            Files.writeString(workDir.resolve("Main.py"), sourceCode, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SandboxException("Could not prepare sandbox work dir", e);
        }

        String container = "gradeflow-" + UUID.randomUUID();
        List<String> cmd = List.of(
                "docker", "run", "--rm", "--name", container,
                "--network", "none", "--user", "10001",
                "-v", workDir.toAbsolutePath() + ":/workspace:ro",
                props.getImage(), "-c", CHECK);

        long start = System.nanoTime();
        try {
            Process p = new ProcessBuilder(cmd).start();
            CompletableFuture<String> out = readAsync(p.getInputStream());
            CompletableFuture<String> err = readAsync(p.getErrorStream());
            if (!p.waitFor(props.getTimeoutSeconds(), TimeUnit.SECONDS)) {
                p.destroyForcibly();
                stopContainer(container);
                throw new SandboxException("Sandbox timed out after " + props.getTimeoutSeconds() + "s");
            }
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            return new SandboxResult(p.exitValue(), out.join(), err.join(), durationMs);
        } catch (IOException e) {
            throw new SandboxException("Could not start the sandbox container", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SandboxException("Sandbox execution was interrupted", e);
        } finally {
            deleteQuietly(workDir);
        }
    }

    private CompletableFuture<String> readAsync(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                byte[] bytes = stream.readNBytes(props.getMaxOutputBytes());
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void stopContainer(String name) {
        try {
            new ProcessBuilder("docker", "kill", name).start().waitFor(5, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException ignored) {
            // best effort; --rm cleans the container up once stopped
        }
    }

    private void deleteQuietly(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // throwaway dir; nothing sensitive remains
        }
    }
}
