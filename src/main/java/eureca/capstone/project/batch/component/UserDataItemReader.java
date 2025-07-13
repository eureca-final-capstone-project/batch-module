package eureca.capstone.project.batch.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class UserDataItemReader implements ItemReader<Long>  {

    private final WebClient userWebClient;
    private Iterator<Long> userIdIterator;
    private boolean initialized = false;
    private final int today = LocalDate.now().getDayOfMonth();

    @Override
    public Long read() throws Exception {
        // 최초 한 번만 호출
        if (!initialized) {
            initializeUserIds();
            initialized = true;
        }

        // Step 종료 조건
        return userIdIterator != null && userIdIterator.hasNext() ? userIdIterator.next() : null;
    }

    private void initializeUserIds() {
        try {
            // 조회 api 호출
            List<Long> userIds = userWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/users/reset-candidates")
                            .queryParam("day", today)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Long>>() {})
                    .block();

            if (userIds != null && !userIds.isEmpty()) {
                this.userIdIterator = userIds.iterator();
            } else { // 데이터 없는 경우
                this.userIdIterator = List.<Long>of().iterator();
            }
        } catch (Exception e) {
            log.error("ItemRead 실패: ", e);
            throw new RuntimeException("ItemRead Failed: ", e);
        }
    }
}

