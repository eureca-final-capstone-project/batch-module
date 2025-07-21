package eureca.capstone.project.batch.component.listener;

import eureca.capstone.project.batch.component.external.DiscordNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {

    private final DiscordNotificationService discordNotificationService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Job 시작 시 특별한 로직이 필요하다면 여기에 구현
    }

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

        // 전체 Step의 Read/Write Count 합산
        long totalReadCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getReadCount).sum();
        long totalWriteCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount).sum();

        // 상세 정보를 포함한 메시지 본문 생성
        String description = String.format(
                """
                *Job* : `%s`
                *Start* : %s
                *End* : %s
                *ReadCount* : %d
                *WriteCount* : %d
                *Exit Status* : %s
                """,
                jobName,
                formatTime(jobExecution.getStartTime()),
                formatTime(jobExecution.getEndTime()),
                totalReadCount,
                totalWriteCount,
                jobExecution.getExitStatus().getExitCode()
        );

        // BATCH_JOB_EXECUTION 테이블에 저장될 메시지
        String exitMessage = String.format("총 %d건의 데이터 처리를 완료했습니다.", totalWriteCount);
        jobExecution.setExitStatus(new ExitStatus("COMPLETED", exitMessage));
        log.info(exitMessage);

        // 디스코드 알림 전송
        discordNotificationService.sendMessage("✅ BATCH-SUCCESS", description, Color.GREEN);
    }

    private void handleFailure(JobExecution jobExecution, String jobName) {
        log.error("!!! JOB FAILED! [{}] has failed.", jobName);

        // 실패한 Step 정보 추출
        StepExecution failedStep = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStatus() == BatchStatus.FAILED)
                .findFirst()
                .orElse(null);

        // 예외 메시지 전체 수집
        String errorMessage = jobExecution.getAllFailureExceptions().stream()
                .map(Throwable::toString)
                .collect(Collectors.joining("\n\n")); // 가독성을 위해 개행문자 2개 사용

        long readCount = failedStep != null ? failedStep.getReadCount() : 0;
        long writeCount = failedStep != null ? failedStep.getWriteCount() : 0;
        String failedStepName = failedStep != null ? failedStep.getStepName() : "N/A";


        // 상세 정보를 포함한 메시지 본문 생성
        String description = String.format(
                """
                *Job* : `%s`
                *Failed Step* : `%s`
                *Start* : %s
                *End* : %s
                *ReadCount (at failure)* : %d
                *WriteCount (at failure)* : %d
                *Exit Status* : %s
                *Error Message* : ```%s```
                """,
                jobName,
                failedStepName,
                formatTime(jobExecution.getStartTime()),
                formatTime(jobExecution.getEndTime()),
                readCount,
                writeCount,
                jobExecution.getExitStatus().getExitCode(),
                errorMessage.isEmpty() ? "No error message provided." : errorMessage
        );

        // BATCH_JOB_EXECUTION 테이블에 저장될 메시지
        String exitMessage = String.format("Job 실패. Failed Step: %s. Error: %s",
                failedStepName,
                jobExecution.getAllFailureExceptions().stream()
                        .map(Throwable::getMessage)
                        .findFirst().orElse("알 수 없는 오류"));
        jobExecution.setExitStatus(new ExitStatus("FAILED", exitMessage));
        log.error(exitMessage);

        // 디스코드 알림 전송
        discordNotificationService.sendMessage("❌ BATCH-FAILURE", description, Color.RED);
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "N/A";
        }
        return time.format(FORMATTER);
    }
}
