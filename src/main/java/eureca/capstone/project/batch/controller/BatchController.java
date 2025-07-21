package eureca.capstone.project.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job resetUserDataJob;
    private final Job restrictionReleaseJob;
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
    public String restartFailedResetUserDataJob() {
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
                        log.error("[restartFailedResetUserDataJob] 재시작 실패: {}", e.getMessage());
                        return "재시작 실패: " + e.getMessage();
                    }

                }
            }
        }
        log.info("[restartFailedResetUserDataJob] 실패한 Job 없음");
        return "실패한 Job이 없음.";
    }

    // 실패한 Job 목록 조회
    @GetMapping("/{jobName}/failures")
    public ResponseEntity<List<Map<String, Object>>> getFailedExecutions(@PathVariable("jobName") String jobName) {
        List<Map<String, Object>> failedExecutionsInfo = new ArrayList<>();

        // jobExplorer를 통해 해당 jobName의 인스턴스를 가져옵니다. (최근 100개)
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 100);

        for (JobInstance instance : jobInstances) {
            List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(instance);
            for (JobExecution execution : jobExecutions) {
                if (execution.getStatus() == BatchStatus.FAILED) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("executionId", execution.getId());
                    info.put("jobName", execution.getJobInstance().getJobName());
                    info.put("startTime", execution.getStartTime());
                    info.put("endTime", execution.getEndTime());
                    info.put("status", execution.getStatus());
                    info.put("jobParameters", execution.getJobParameters().toString());
                    failedExecutionsInfo.add(info);
                }
            }
        }
        return ResponseEntity.ok(failedExecutionsInfo);
    }

    //만료된 사용자 제재를 해제하는 배치
    @PostMapping("/run-restriction-release")
    public String runRestrictionReleaseJobManual() {
        log.info("[runRestrictionReleaseJobManual] 사용자 제재 해제 배치 수동 실행");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("now", LocalDateTime.now().toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(restrictionReleaseJob, jobParameters);

            log.info("[runRestrictionReleaseJobManual] 사용자 제재 해제 배치 수동 실행 완료");
            return "사용자 제재 해제 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runRestrictionReleaseJobManual] 사용자 제재 해제 배치 수동 실행 실패", e);
            return "사용자 제재 해제 배치 수동 실행 실패: " + e.getMessage();
        }
    }
}