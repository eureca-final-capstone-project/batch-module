package eureca.capstone.project.batch.slack.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SlackMessageRequestDto {
    String jobName;
    String stepName;
    Long readCount;
    Long writeCount;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String exitSatus;
    String errorMessage;
}
