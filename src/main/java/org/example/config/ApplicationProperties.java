package org.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter @Setter
public class ApplicationProperties {
    private ExecuteConfig executeConfig = new ExecuteConfig();

    @Getter @Setter
    public static class ExecuteConfig {
        private int maxThread = 100;
        private int timeoutExecuteMilli = 300000;
    }
}
