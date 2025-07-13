package eureca.capstone.project.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient userWebClient() {
        return WebClient.builder()
                .baseUrl("http://43.203.66.126:8080") // 테스트용
                .build();
    }
}

