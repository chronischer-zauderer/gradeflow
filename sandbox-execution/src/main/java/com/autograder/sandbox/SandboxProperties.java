package com.autograder.sandbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Sandbox limits, overridable via SANDBOX_* env vars. */
@ConfigurationProperties(prefix = "sandbox")
public class SandboxProperties {

    private String image = "gradeflow-python-sandbox";
    private int timeoutSeconds = 10;
    private int maxOutputBytes = 65536;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(int maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }
}
