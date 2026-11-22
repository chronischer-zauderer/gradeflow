package com.autograder.sandbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Sandbox limits, overridable via SANDBOX_* env vars. */
@ConfigurationProperties(prefix = "sandbox")
public class SandboxProperties {

    private String image = "gradeflow-python-sandbox";
    private int timeoutSeconds = 10;
    private int maxOutputBytes = 65536;
    private String memory = "256m";
    private String memorySwap = "256m";
    private String cpus = "0.5";
    private int pidsLimit = 64;
    private boolean readOnly = true;
    private String tmpfs = "/tmp:rw,noexec,nosuid,size=64m";

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

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getMemorySwap() {
        return memorySwap;
    }

    public void setMemorySwap(String memorySwap) {
        this.memorySwap = memorySwap;
    }

    public String getCpus() {
        return cpus;
    }

    public void setCpus(String cpus) {
        this.cpus = cpus;
    }

    public int getPidsLimit() {
        return pidsLimit;
    }

    public void setPidsLimit(int pidsLimit) {
        this.pidsLimit = pidsLimit;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getTmpfs() {
        return tmpfs;
    }

    public void setTmpfs(String tmpfs) {
        this.tmpfs = tmpfs;
    }
}
