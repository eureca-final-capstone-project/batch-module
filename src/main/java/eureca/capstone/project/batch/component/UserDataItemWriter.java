package eureca.capstone.project.batch.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class UserDataItemWriter implements ItemWriter<Long> {

    @Value("#{jobParameters['amount']}")
    private int amount;

    private final WebClient userWebClient;


    @Override
    public void write(Chunk<? extends Long> chunk) throws Exception {
        List<Long> userIds = chunk.getItems()
                .stream().collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        try {
            userWebClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/users/reset-monthly")
                            .queryParam("amount", amount)
                            .build())
//                    .uri("/api/users/reset-monthly")
                    .bodyValue(userIds)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("처리 개수: {}", userIds.size());

        } catch (Exception e) {
            log.error("ItemWrite 실패: {}", userIds, e);
            throw new RuntimeException("ItemWrite Failed", e);
        }
    }
}