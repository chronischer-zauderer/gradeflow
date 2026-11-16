package com.autograder.aievaluation;

import com.autograder.shared.contracts.AiEvaluationPort;
import com.autograder.shared.contracts.AiFeedbackResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies a neutral feedback adapter until an AI evaluation implementation is registered. */
@Configuration
public class DefaultAiEvaluationConfiguration {

    @Bean
    @ConditionalOnMissingBean(AiEvaluationPort.class)
    AiEvaluationPort defaultAiEvaluationPort() {
        return (artifact, grading) -> new AiFeedbackResult(
                "AI feedback not configured", java.util.List.of(), 0, "none");
    }
}
