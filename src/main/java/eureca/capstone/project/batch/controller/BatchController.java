package eureca.capstone.project.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job resetUserDataJob;
    private final JobOperator jobOperator;
    private final JobExplorer jobExplorer;

    // 월 데이터 초기화
    @PostMapping("/reset-data")
    public String runResetDataBatchManual() {
        try {
            log.info("[runResetDataBatchManual] 사용자 데이터 초기화 배치 수동 실행");
            jobLauncher.run(resetUserDataJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("currentDate", LocalDate.now().toString())
                    .toJobParameters());
            log.info("[runResetDataBatchManual] 사용자 데이터 초기화 배치 수동 실행 완료");
            return "사용자 데이터 초기화 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runResetDataBatchManual] 사용자 데이터 초기화 배치 수동 실행 실패", e);
            return "사용자 데이터 초기화 배치 수동 실행 실패: " + e.getMessage();
        }
    }

    // 실패한 배치 재시작
    @PostMapping("/restart-failed")
    public String restartFailedResetUserDataJob() throws Exception {
        String jobName = "resetUserDataJob";
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 10);

        for (JobInstance instance : jobInstances) {
            // 실행기록 조회
            List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(instance);

            for (JobExecution execution : jobExecutions) {
                if (execution.getStatus() == BatchStatus.FAILED) {
                    try{
                        Long failedId = execution.getId();
                        Long restartedId = jobOperator.restart(failedId);
                        log.info("[restartFailedResetUserDataJob] 재시작 성공. 기존 executionId: {}, 재시작 executionId: {}", failedId, restartedId);
                        return "재시작 성공: " + restartedId;
                    } catch (Exception e){
                        log.error("[restartFailedResetUserDataJob] 재시작 실패: ", e.getMessage());
                        return "재시작 실패: " + e.getMessage();
                    }

                }
            }
        }
        log.info("[restartFailedResetUserDataJob] 실패한 Job 없음");
        return "실패한 Job이 없음.";
    }
}