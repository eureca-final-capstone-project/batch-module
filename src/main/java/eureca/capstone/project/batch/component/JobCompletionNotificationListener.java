package eureca.capstone.project.batch.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! JOB FINISHED! Time to verify the results");

            long totalWriteCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount)
                    .sum();

            String exitMessage = String.format("총 %d건의 판매글 만료 처리 완료.", totalWriteCount);

            // 새로운 ExitStatus를 생성하여 JobExecution에 설정합니다.
            // 두 번째 인자인 description이 BATCH_JOB_EXECUTION 테이블의 EXIT_MESSAGE 컬럼에 저장됩니다.
            jobExecution.setExitStatus(new org.springframework.batch.core.ExitStatus("COMPLETED", exitMessage));
            log.info(exitMessage);
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("!!! JOB FAILED with exceptions");

            // Job 실행 중 발생한 모든 예외를 가져와 메시지를 만듭니다.
            String errorMessage = jobExecution.getAllFailureExceptions().stream()
                    .map(Throwable::toString)
                    .collect(Collectors.joining("\n"));

            // DB에 저장될 EXIT_MESSAGE에 실제 에러 내용을 포함시킵니다.
            String exitMessage = "작업 처리 중 오류 발생\n" + errorMessage;
            jobExecution.setExitStatus(new ExitStatus("FAILED", exitMessage));
        }
    }
}
