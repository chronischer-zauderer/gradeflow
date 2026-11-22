package com.autograder.grading;

import com.autograder.shared.contracts.GradingPort;
import com.autograder.shared.contracts.GradingResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies a neutral grading adapter until a real grading implementation is registered. */
@Configuration
public class DefaultGradingConfiguration {

    @Bean
    @ConditionalOnMissingBean(GradingPort.class)
    GradingPort defaultGradingPort() {
        return artifact -> new GradingResult(
                0, 0, artifact.compiled(), java.util.List.of(), "Grading not configured");
    }
}
