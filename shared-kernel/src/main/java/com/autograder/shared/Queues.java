package com.autograder.shared;

/** Names of the RabbitMQ destinations shared by app-api (producer) and app-worker (consumer). */
public final class Queues {
    public static final String SUBMISSIONS = "gradeflow.submissions";

    private Queues() {
    }
}
