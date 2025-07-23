package eureca.capstone.project.batch.common.service;

import eureca.capstone.project.batch.common.dto.AlarmCreationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducerService {
    private static final String TOPIC = "notification";
    private final KafkaTemplate<String, AlarmCreationDto> kafkaTemplate;

    public void send(AlarmCreationDto creationDto) {
        log.info("[send] 카프카 토픽: {}, 메시지: {}", TOPIC, creationDto.getContent());
        kafkaTemplate.send(TOPIC, creationDto);
    }
}
