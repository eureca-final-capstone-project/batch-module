package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.component.external.DiscordNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {

    private final DiscordNotificationService discordNotificationService;
    private final Environment environment; // 실행 환경(profile)을 가져오기 위해 주입
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            handleSuccess(jobExecution, jobName);
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            handleFailure(jobExecution, jobName);
        }
    }

    private void handleSuccess(JobExecution jobExecution, String jobName) {
        log.info("!!! JOB FINISHED! [{}] has completed successfully.", jobName);

        long totalWriteCount = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();

        if (totalWriteCount == 0) {
            // 처리한 데이터가 없는 경우 정보성 알림 전송
            String description = createBaseDescription(jobExecution, jobName) +
                    String.format("\n*Summary* : 처리할 데이터가 없습니다.");

            discordNotificationService.sendMessage("ℹ️ BATCH-INFO", description, Color.BLUE);
        } else {
            // 데이터 처리에 성공한 경우 성공 알림 전송
            String description = createBaseDescription(jobExecution, jobName) +
                    String.format("\n*Summary* : 총 %d건의 데이터 처리를 완료했습니다.", totalWriteCount);

            discordNotificationService.sendMessage("✅ BATCH-SUCCESS", description, Color.GREEN);
        }

        // BATCH_JOB_EXECUTION 테이블에 저장될 메시지 업데이트
        String exitMessage = String.format("총 %d건의 데이터 처리를 완료했습니다.", totalWriteCount);
        jobExecution.setExitStatus(new ExitStatus("COMPLETED", exitMessage));
        log.info(exitMessage);
    }

    private void handleFailure(JobExecution jobExecution, String jobName) {
        log.error("!!! JOB FAILED! [{}] has failed.", jobName);

        StepExecution failedStep = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStatus() == BatchStatus.FAILED).findFirst().orElse(null);

        String errorMessage = jobExecution.getAllFailureExceptions().stream()
                .map(Throwable::toString).collect(Collectors.joining("\n\n"));

        String failedStepName = failedStep != null ? failedStep.getStepName() : "N/A";

        String description = createBaseDescription(jobExecution, jobName) +
                String.format(
                        """
                        *Failed Step* : `%s`
                        *Error Message* : ```%s```
                        """,
                        failedStepName,
                        errorMessage.isEmpty() ? "No error message provided." : errorMessage
                );

        String exitMessage = String.format("Job 실패. Failed Step: %s. Error: %s",
                failedStepName,
                jobExecution.getAllFailureExceptions().stream()
                        .map(Throwable::getMessage).findFirst().orElse("알 수 없는 오류"));
        jobExecution.setExitStatus(new ExitStatus("FAILED", exitMessage));
        log.error(exitMessage);

        discordNotificationService.sendMessage("❌ BATCH-FAILURE", description, Color.RED);
    }

    // 공통 정보 생성 부분을 별도 메서드로 추출
    private String createBaseDescription(JobExecution jobExecution, String jobName) {
        String profiles = Arrays.toString(environment.getActiveProfiles());
        String jobParameters = jobExecution.getJobParameters().toString();

        return String.format(
                """
                *Environment* : `%s`
                *Job* : `%s`
                *Parameters* : `%s`
                ----------------------------------
                *Start* : %s
                *End* : %s
                *ReadCount* : %d
                *WriteCount* : %d
                *Exit Status* : %s
                """,
                profiles.isEmpty() ? "default" : profiles,
                jobName,
                jobParameters,
                formatTime(jobExecution.getStartTime()),
                formatTime(jobExecution.getEndTime()),
                jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum(),
                jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum(),
                jobExecution.getExitStatus().getExitCode()
        );
    }

    private String formatTime(LocalDateTime time) {
        return (time != null) ? time.format(FORMATTER) : "N/A";
    }
}