package com.autograder.sandbox;

/** Controlled outcome of running student code in the sandbox. */
public record SandboxResult(int exitCode, String stdout, String stderr, long durationMilliseconds) {

    public boolean compiled() {
        return exitCode == 0;
    }
}
