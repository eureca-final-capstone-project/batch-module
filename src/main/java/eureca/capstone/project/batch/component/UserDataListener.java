package eureca.capstone.project.batch.component;

import eureca.capstone.project.batch.slack.dto.SlackMessageRequestDto;
import eureca.capstone.project.batch.slack.service.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDataListener implements StepExecutionListener, JobExecutionListener {

    private final SlackService slackService;

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        Long readCount = stepExecution.getReadCount();
        Long writeCount = stepExecution.getWriteCount();

        if(stepExecution.getStatus() == BatchStatus.FAILED) {
            String errorMessage = "알 수 없는 오류";

            if (!stepExecution.getFailureExceptions().isEmpty()) {
                Throwable rootCause = stepExecution.getFailureExceptions().get(0);
                while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                    rootCause = rootCause.getCause();
                }
                String fullStackTrace = Arrays.stream(rootCause.getStackTrace())
                        .limit(5) // 또는 10줄
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
            log.error("배치 실패. Job: {}, Step: {}, 오류: {}", stepExecution.getJobExecution().getJobInstance().getJobName(),
                    stepExecution.getStepName(), errorMessage);

            return stepExecution.getExitStatus();
        } else{
            log.info("배치 성공. Job: {}, Step: {}, Read: {}, Write: {}", stepExecution.getJobExecution().getJobInstance().getJobName(),
                    stepExecution.getStepName(), readCount, writeCount);

            String msg = String.format("UserData 초기화 완료 - 총 read: %d, write: %d", readCount, writeCount);
            return new ExitStatus(ExitStatus.COMPLETED.getExitCode(), msg);
        }
    }
}
