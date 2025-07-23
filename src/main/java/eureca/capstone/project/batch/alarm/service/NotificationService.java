package eureca.capstone.project.batch.alarm.service;

import eureca.capstone.project.batch.alarm.dto.AlarmCreationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String NOTIFICATION_TOPIC = "notification";
    private final KafkaTemplate<String, AlarmCreationDto> kafkaTemplate;

    public void sendNotification(Long userId, String alarmType, String content) {
        AlarmCreationDto alarm = AlarmCreationDto.builder()
                .userId(userId)
                .alarmType(alarmType)
                .content(content)
                .build();
        kafkaTemplate.send(NOTIFICATION_TOPIC, alarm);
        log.info("Kafka notification sent: userId={}, alarmType={}, content={}", userId, alarmType, content);
    }
}
