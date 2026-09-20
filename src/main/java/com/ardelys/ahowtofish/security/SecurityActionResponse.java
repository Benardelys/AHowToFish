package com.ardelys.ahowtofish.security;

public enum SecurityActionResponse {
    IGNORE,
    LOG,
    BLOCK,
    LOG_AND_BLOCK,
    ALERT,
    LOG_BLOCK_AND_ALERT;

    public boolean shouldLog() {
        return this == LOG || this == LOG_AND_BLOCK || this == LOG_BLOCK_AND_ALERT;
    }

    public boolean shouldBlock() {
        return this == BLOCK || this == LOG_AND_BLOCK || this == LOG_BLOCK_AND_ALERT;
    }

    public boolean shouldAlert() {
        return this == ALERT || this == LOG_BLOCK_AND_ALERT;
    }
}
