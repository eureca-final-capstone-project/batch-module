package eureca.capstone.project.batch.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import eureca.capstone.project.batch.dto.UserDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class UserDataItemWriter implements ItemWriter<Long> {

    @Value("#{jobParameters['amount']}")
    private int amount;

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> userResetKafkaTemplate;
    private static final String USER_RESET_TOPIC = "reset-user-data-topic";

    @Override
    public void write(Chunk<? extends Long> chunk) throws Exception {
        List<Long> userIds = chunk.getItems()
                .stream().collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        try {
            UserDataDto userDataDto = new UserDataDto(userIds,amount);
            String jsonUserIds = objectMapper.writeValueAsString(userDataDto);

            SendResult<String, String> result =
                    userResetKafkaTemplate.send(USER_RESET_TOPIC, jsonUserIds).get();
            log.info("topic: {}", USER_RESET_TOPIC);
            log.info("처리 개수: {}", userIds.size());

        } catch (Exception e) {
            log.error("카프카 전송 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}