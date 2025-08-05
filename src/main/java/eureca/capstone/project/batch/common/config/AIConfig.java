package eureca.capstone.project.batch.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    ChatClient keywordExtractionClient(ChatClient.Builder builder){
        return builder.build();
    }
}
