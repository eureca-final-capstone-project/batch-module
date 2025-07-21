package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.component.external.DiscordNotificationService;
import eureca.capstone.project.batch.slack.dto.SlackMessageRequestDto;
import eureca.capstone.project.batch.slack.service.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDataListener implements StepExecutionListener, JobExecutionListener {

    private final SlackService slackService;
    private final DiscordNotificationService discordNotificationService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();
        Long readCount = stepExecution.getReadCount();
        Long writeCount = stepExecution.getWriteCount();

        if(stepExecution.getStatus() == BatchStatus.FAILED) {
            String errorMessage = "알 수 없는 오류";

            if (!stepExecution.getFailureExceptions().isEmpty()) {
                // 에러메세지 탐색
                Throwable rootCause = stepExecution.getFailureExceptions().get(0);
                while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                    rootCause = rootCause.getCause();
                }
                String fullStackTrace = Arrays.stream(rootCause.getStackTrace())
                        .limit(5)
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));

                errorMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage() + "\n```\n```\n"
                        + fullStackTrace + "\n...";

            }

            SlackMessageRequestDto message = SlackMessageRequestDto.builder()
                    .jobName(stepExecution.getJobExecution().getJobInstance().getJobName())
                    .stepName(stepExecution.getStepName())
                    .readCount(readCount)
                    .writeCount(writeCount)
                    .startTime(stepExecution.getStartTime())
                    .endTime(stepExecution.getEndTime())
                    .exitSatus(stepExecution.getExitStatus().getExitCode())
                    .errorMessage(errorMessage)
                    .build();

            slackService.sendMessage(message);
            String discordTitle = String.format("❌ BATCH-STEP-FAILURE : %s", jobName);
            String discordDescription = String.format(
                    """
                    *Step* : `%s`
                    *Start* : %s
                    *End* : %s
                    *Read/Write* : %d / %d
                    *Status* : `%s`
                    ---
                    **Error Message**
                    %s
                    ```
         
                    """,
                    stepName,
                    stepExecution.getStartTime().format(FORMATTER),
                    stepExecution.getEndTime().format(FORMATTER),
                    readCount,
                    writeCount,
                    stepExecution.getExitStatus().getExitCode(),
                    errorMessage
            );
            discordNotificationService.sendMessage(discordTitle, discordDescription, Color.RED);
            log.error("배치 실패. Job: {}, Step: {}, 오류: {}", stepExecution.getJobExecution().getJobInstance().getJobName(),
                    stepExecution.getStepName(), errorMessage);

            return stepExecution.getExitStatus();
        } else{
            log.info("배치 성공. Job: {}, Step: {}, Read: {}, Write: {}", stepExecution.getJobExecution().getJobInstance().getJobName(),
                    stepExecution.getStepName(), readCount, writeCount);

            // Discord 성공 알림 전송
            String discordTitle = String.format("✅ BATCH-STEP-SUCCESS : %s", jobName);
            String discordDescription = String.format(
                    """
                    *Step* : `%s`
                    *ReadCount* : %d
                    *WriteCount* : %d
                    *CommitCount* : %d
                    *Status* : `%s`
                    """,
                    stepName,
                    readCount,
                    writeCount,
                    stepExecution.getCommitCount(),
                    stepExecution.getExitStatus().getExitCode()
            );
            discordNotificationService.sendMessage(discordTitle, discordDescription, Color.BLUE);

            String msg = String.format("UserData 초기화 완료 - 총 read: %d, write: %d", readCount, writeCount);
            return new ExitStatus(ExitStatus.COMPLETED.getExitCode(), msg);
        }
    }
}
