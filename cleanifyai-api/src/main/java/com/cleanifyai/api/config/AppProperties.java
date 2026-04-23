package com.cleanifyai.api.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Seed seed = new Seed();
    private final Integracoes integracoes = new Integracoes();
    private final Security security = new Security();

    public Cors getCors() {
        return cors;
    }

    public Seed getSeed() {
        return seed;
    }

    public Integracoes getIntegracoes() {
        return integracoes;
    }

    public Security getSecurity() {
        return security;
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Seed {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Integracoes {
        private boolean whatsappEnabled;
        private boolean iaEnabled;

        public boolean isWhatsappEnabled() {
            return whatsappEnabled;
        }

        public void setWhatsappEnabled(boolean whatsappEnabled) {
            this.whatsappEnabled = whatsappEnabled;
        }

        public boolean isIaEnabled() {
            return iaEnabled;
        }

        public void setIaEnabled(boolean iaEnabled) {
            this.iaEnabled = iaEnabled;
        }
    }

    public static class Security {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }
    }

    public static class Jwt {
        private String secret = "cleanifyai-jwt-secret-chave-com-32-caracteres-minimos";
        private long expirationSeconds = 43200;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }
    }
}

