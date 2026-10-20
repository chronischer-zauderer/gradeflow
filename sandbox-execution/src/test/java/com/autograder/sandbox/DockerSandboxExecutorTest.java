package com.autograder.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercises the real Docker sandbox. Skipped when Docker or the image is unavailable. */
class DockerSandboxExecutorTest {

    private DockerSandboxExecutor executor;
    private SandboxProperties props;

    @BeforeEach
    void setUp() {
        assumeTrue(imageAvailable(), "Docker image gradeflow-python-sandbox not available");
        props = new SandboxProperties();
        executor = new DockerSandboxExecutor(props);
    }

    @Test
    void validPythonCompiles() {
        SandboxResult r = executor.compile("print('Hola GradeFlow')\n");
        assertThat(r.compiled()).isTrue();
        assertThat(r.exitCode()).isZero();
    }

    @Test
    void capturesStdoutFromPrintAndLoop() {
        SandboxResult r = executor.compile("for i in range(3):\n    print('line', i)\n");
        assertThat(r.compiled()).isTrue();
        assertThat(r.stdout()).contains("line 0").contains("line 2");
    }

    @Test
    void invalidPythonFailsWithStderr() {
        SandboxResult r = executor.compile("def broken(:\n");
        assertThat(r.compiled()).isFalse();
        assertThat(r.stderr()).contains("Error");
    }

    @Test
    void timeoutRaisesSandboxException() {
        props.setTimeoutSeconds(0); // forces the timeout branch deterministically
        assertThatThrownBy(() -> executor.compile("print('hi')"))
                .isInstanceOf(SandboxException.class);
    }

    private static boolean imageAvailable() {
        try {
            Process p = new ProcessBuilder("docker", "image", "inspect", "gradeflow-python-sandbox")
                    .redirectErrorStream(true).start();
            return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
