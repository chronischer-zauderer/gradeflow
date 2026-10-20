package com.autograder.appworker;

import com.autograder.orchestrator.EvaluationService;
import com.autograder.shared.Queues;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EvaluationListener {

    private final EvaluationService evaluation;

    public EvaluationListener(EvaluationService evaluation) {
        this.evaluation = evaluation;
    }

    @RabbitListener(queues = Queues.SUBMISSIONS)
    public void onSubmission(String submissionId) {
        evaluation.evaluate(UUID.fromString(submissionId));
    }
}
