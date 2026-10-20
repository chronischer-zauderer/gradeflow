package com.autograder.sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Compiles student Python in a throwaway Docker container: no network, non-root user, time limit,
 * captured output. The source is piped in on stdin and checked with the {@code compile()} builtin
 * (equivalent compile check to {@code python -m py_compile}), so nothing touches the filesystem and
 * the behaviour is identical on every host platform.
 */
@Component
public class DockerSandboxExecutor implements SandboxExecutor {

    // Compile AND run the student code so its stdout/stderr (print, loops) is captured. A syntax
    // error fails at compile; a runtime error fails at exec — both give a non-zero exit.
    private static final String CHECK = "import sys; exec(compile(sys.stdin.buffer.read(), 'Main.py', 'exec'), {'__name__': '__main__'})";

    private final SandboxProperties props;

    public DockerSandboxExecutor(SandboxProperties props) {
        this.props = props;
    }

    @Override
    public SandboxResult compile(String sourceCode) {
        String container = "gradeflow-" + UUID.randomUUID();
        List<String> cmd = List.of(
                "docker", "run", "--rm", "-i", "--name", container,
                "--network", "none", "--user", "10001",
                props.getImage(), "-c", CHECK);

        long start = System.nanoTime();
        try {
            Process p = new ProcessBuilder(cmd).start();
            writeStdin(p, sourceCode); // closes stdin so python reaches EOF and compiles
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
        }
    }

    private void writeStdin(Process p, String sourceCode) {
        try (OutputStream in = p.getOutputStream()) {
            in.write(sourceCode.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // container may have exited early; exit code + stderr carry the outcome
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
}
