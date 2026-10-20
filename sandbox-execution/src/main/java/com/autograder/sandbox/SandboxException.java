package com.autograder.sandbox;

/** Infrastructure failure while running the sandbox (not a compile error of the student code). */
public class SandboxException extends RuntimeException {
    public SandboxException(String message, Throwable cause) {
        super(message, cause);
    }

    public SandboxException(String message) {
        super(message);
    }
}
