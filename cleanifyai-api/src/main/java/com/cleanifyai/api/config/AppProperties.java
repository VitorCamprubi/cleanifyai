package com.cleanifyai.api.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Seed seed = new Seed();
    private final Integracoes integracoes = new Integracoes();

    public Cors getCors() {
        return cors;
    }

    public Seed getSeed() {
        return seed;
    }

    public Integracoes getIntegracoes() {
        return integracoes;
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
}

