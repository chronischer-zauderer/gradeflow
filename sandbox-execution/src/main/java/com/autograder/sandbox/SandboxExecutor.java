package com.autograder.sandbox;

/** Runs a compilation check on student source in an isolated environment. */
public interface SandboxExecutor {

    /**
     * @param sourceCode the student's source
     * @return the controlled result (exit code, captured output, duration)
     * @throws SandboxException on infrastructure failure (container could not run, timeout)
     */
    SandboxResult compile(String sourceCode);
}
