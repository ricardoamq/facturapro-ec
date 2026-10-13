package com.facturapro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Admin admin,
        Demo demo,
        Login login,
        Password password,
        Mail mail) {

    public record Cors(String allowedOrigins) {
    }

    public record Admin(String initialUsername, String initialPassword, String initialFullName, String initialEmail) {
    }

    public record Demo(boolean enabled) {
    }

    public record Login(int maxFailedAttempts, int lockMinutes) {
    }

    public record Password(int resetTtlMinutes, int minLength) {
    }

    public record Mail(boolean devLoggingEnabled) {
    }
}
