package eureca.capstone.project.batch.controller;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import eureca.capstone.project.batch.common.service.BatchFailureLogService;
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
    private final JobOperator jobOperator;
    private final JobExplorer jobExplorer;

    private final Job resetUserDataJob;
    private final Job restrictionReleaseJob;
    private final Job expireGeneralSaleFeedJob;

    private final BatchFailureLogService batchFailureLogService;

    // 월 데이터 초기화
    @PostMapping("/run-reset-data")
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

    // 만료된 사용자 제재를 해제하는 배치
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

    // 게시글 기간만료 배치
    @PostMapping("/run-expire-feed")
    public String runExpireGeneralSaleFeedJobManual() {
        log.info("[runExpireGeneralSaleFeedJobManual] 게시글 기간만료 배치 수동 실행");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDateTime", LocalDateTime.now().toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(expireGeneralSaleFeedJob, jobParameters);

            log.info("[runExpireGeneralSaleFeedJobManual] 게시글 기간만료 배치 수동 실행 완료");
            return "게시글 기간만료 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runExpireGeneralSaleFeedJobManual] 게시글 기간만료 배치 수동 실행 실패", e);
            return "게시글 기간만료 배치 수동 실행 실패: " + e.getMessage();
        }
    }


    /**
     * 재처리가 필요한 실패/스킵 로그 목록을 조회합니다.
     * @param jobName (선택) 특정 Job의 실패 내역만 필터링
     * @return 재처리되지 않은 실패 로그 목록
     */
    @GetMapping("/failures")
    public ResponseEntity<List<BatchFailureLog>> getUnreprocessedFailures(
            @RequestParam(required = false) String jobName) {
        // reprocessed가 false인 로그만 조회
        List<BatchFailureLog> failures = batchFailureLogService.getFailures(jobName, null, null, false);
        return ResponseEntity.ok(failures);
    }

    /**
     * 특정 실패 로그 아이템을 재처리합니다.
     * @param failureLogId 재처리할 batch_failure_log의 ID
     * @return 처리 결과
     */
    @PostMapping("/failures/{failureLogId}/reprocess")
    public ResponseEntity<String> reprocessFailedItem(@PathVariable Long failureLogId) {
        log.info("[reprocessFailedItem] 실패 항목 재처리 요청. FailureLog ID: {}", failureLogId);
        try {
            batchFailureLogService.reprocessFailedItem(failureLogId);
            return ResponseEntity.ok("재처리 성공. Log ID: " + failureLogId);
        } catch (Exception e) {
            log.error("[reprocessFailedItem] 실패 항목 재처리 중 오류 발생. Log ID: {}", failureLogId, e);
            // 클라이언트에게 에러 원인을 알려주는 것이 좋습니다.
            return ResponseEntity.badRequest().body("재처리 실패: " + e.getMessage());
        }
    }


    // 실패한 월 데이터 배치 배치 재시작
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

    /**
     * 특정 Job의 실패한 실행(Execution) 목록을 조회합니다.
     * @param jobName 조회할 Job의 이름
     * @return 실패한 Job Execution 목록
     */
    @GetMapping("/{jobName}/executions/failed")
    public ResponseEntity<List<Map<String, Object>>> getFailedExecutions(@PathVariable String jobName) {
        List<Map<String, Object>> failedExecutionsInfo = new ArrayList<>();
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 100);

        for (JobInstance instance : jobInstances) {
            for (JobExecution execution : jobExplorer.getJobExecutions(instance)) {
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

    /**
     * 특정 Job Execution ID를 사용하여 실패한 배치를 재시작합니다.
     * @param executionId 재시작할 Job Execution의 ID
     * @return 재시작 결과
     */
    @PostMapping("/executions/{executionId}/restart")
    public ResponseEntity<String> restartFailedJob(@PathVariable Long executionId) {
        try {
            Long restartedExecutionId = jobOperator.restart(executionId);
            String message = String.format("재시작 성공. 기존 Execution ID: %d, 새로운 Execution ID: %d", executionId, restartedExecutionId);
            log.info("[restartFailedJob] {}", message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("[restartFailedJob] 재시작 실패. Execution ID: {}", executionId, e);
            return ResponseEntity.internalServerError().body("재시작에 실패했습니다: " + e.getMessage());
        }
    }
}
