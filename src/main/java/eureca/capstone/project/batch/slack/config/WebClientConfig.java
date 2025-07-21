package eureca.capstone.project.batch.slack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://slack.com/api")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
