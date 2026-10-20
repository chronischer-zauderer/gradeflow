package com.autograder.submissions;

/** Domain errors for the submission flow. {@code notFound} maps to HTTP 404, the rest to 400. */
public class SubmissionException extends RuntimeException {

    private final boolean notFound;

    private SubmissionException(String message, boolean notFound) {
        super(message);
        this.notFound = notFound;
    }

    public static SubmissionException validation(String message) {
        return new SubmissionException(message, false);
    }

    public static SubmissionException notFound(String message) {
        return new SubmissionException(message, true);
    }

    public boolean isNotFound() {
        return notFound;
    }
}
