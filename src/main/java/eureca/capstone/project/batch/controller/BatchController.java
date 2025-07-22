package eureca.capstone.project.batch.controller;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import eureca.capstone.project.batch.common.service.BatchFailureLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;

@Tag(name = "Batch", description = "배치 작업 관리 API")
@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final JobOperator jobOperator;
    private final JobExplorer jobExplorer;

    // Job Beans
    private final Job resetUserDataJob;
    private final Job transactionStatisticJob;
    private final Job restrictionReleaseJob;
    private final Job expireGeneralSaleFeedJob;
    private final Job auctionProcessingJob;
    private final Job expireDataCouponJob;
    private final Job expireEventCouponJob;

    // Service for reprocessing
    private final BatchFailureLogService batchFailureLogService;

    @Operation(summary = "월 데이터 초기화 배치 수동 실행", description = "매월 1일에 실행되는 사용자 데이터 초기화 배치를 수동으로 즉시 실행합니다.")
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

    @Operation(summary = "거래량/시세 통계 배치 수동 실행", description = "매 시간마다 이전시간의 거래량과 통신사별 시세 통계를 내리는 배치를 수동으로 즉시 실행합니다.")
    @PostMapping("/statistic-transaction")
    public String runStatisticBatchManual() {
        try {
            log.info("[runStatisticBatchManual] 거래량/시세 통계 배치 수동 실행");
            jobLauncher.run(transactionStatisticJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("currentTime", LocalDateTime.now().toString())
                    .toJobParameters());
            log.info("[runStatisticBatchManual] 거래량/시세 통계 배치 수동 실행 완료");
            return "거래량/시세 통계 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runStatisticBatchManual] 거래량/시세 통계 배치 수동 실행 실패", e);
            return "거래량/시세 통계 배치 수동 실행 실패: " + e.getMessage();
        }
    }

    @Operation(summary = "사용자 제재 해제 배치 수동 실행", description = "만료된 사용자 제재(글쓰기 금지 등)를 해제하는 배치를 수동으로 즉시 실행합니다.")
    @PostMapping("/run-restriction-release")
    public String runRestrictionReleaseJobManual() {
        log.info("[runRestrictionReleaseJobManual] 사용자 제재 해제 배치 수동 실행");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("now", LocalDateTime.now())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(restrictionReleaseJob, jobParameters);
            return "사용자 제재 해제 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runRestrictionReleaseJobManual] 사용자 제재 해제 배치 수동 실행 실패", e);
            return "사용자 제재 해제 배치 수동 실행 실패: " + e.getMessage();
        }
    }

    @Operation(summary = "게시글 기간만료 배치 수동 실행", description = "기간이 만료된 일반 판매 게시글의 상태를 변경하는 배치를 수동으로 즉시 실행합니다.")
    @PostMapping("/run-expire-feed")
    public String runExpireGeneralSaleFeedJobManual() {
        log.info("[runExpireGeneralSaleFeedJobManual] 게시글 기간만료 배치 수동 실행");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDateTime", LocalDateTime.now().toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(expireGeneralSaleFeedJob, jobParameters);
            return "게시글 기간만료 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runExpireGeneralSaleFeedJobManual] 게시글 기간만료 배치 수동 실행 실패", e);
            return "게시글 기간만료 배치 수동 실행 실패: " + e.getMessage();
        }
    }


    @Operation(summary = "데이터 충전권 기간만료 배치 수동 실행", description = "기간이 만료된 데이터 충전권을 기간만료 상태로 변경하는 배치를 수동으로 즉시 실행합니다.")
    @PostMapping("/expire-data-coupon")
    public String runExpireDataCouponBatchManual() {
        try {
            log.info("[runExpireDataCouponBatchManual] 데이터 충전권 기간만료 배치 수동 실행");
            jobLauncher.run(expireDataCouponJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("currentTime", LocalDateTime.now().toString())
                    .toJobParameters());
            log.info("[runExpireDataCouponBatchManual] 데이터 충전권 기간만료 배치 수동 실행 완료");
            return "데이터 충전권 기간만료 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runExpireDataCouponBatchManual] 데이터 충전권 기간만료 배치 수동 실행 실패", e);
            return "데이터 충전권 기간만료 배치 수동 실행 실패: " + e.getMessage();
        }
    }


    @Operation(summary = "이벤트 쿠폰 기간만료 배치 수동 실행", description = "기간이 만료된 이벤트 쿠폰을 기간만료 상태로 변경하는 배치를 수동으로 즉시 실행합니다.")
    @PostMapping("/expire-event-coupon")
    public String runExpireEventCouponBatchManual() {
        try {
            log.info("[runExpireEventCouponBatchManual] 이벤트 쿠폰 기간만료 배치 수동 실행");
            jobLauncher.run(expireEventCouponJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("currentTime", LocalDateTime.now().toString())
                    .toJobParameters());
            log.info("[runExpireEventCouponBatchManual] 이벤트 쿠폰 기간만료 배치 수동 실행 완료");
            return "이벤트 쿠폰 기간만료 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runExpireEventCouponBatchManual] 이벤트 쿠폰 기간만료 배치 수동 실행 실패", e);
            return "이벤트 쿠폰 기간만료 배치 수동 실행 실패: " + e.getMessage();
        }
    }

    @Operation(summary = "입찰 판매 낙찰 배치 수동 실행", description = "입찰 판매글을 낙찰 처리하는 배치를 수동으로 즉시 실행합니다.")
    @PostMapping("/run-auction-processing")
    public String runAuctionProcessingJobManual() {
        log.info("[runAuctionProcessingJobManual] 경매 처리 배치 수동 실행");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("targetDateTime", LocalDateTime.now())
                    .addLong("salesTypeId", 2L) // 2L이 입찰판매
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(auctionProcessingJob, jobParameters);
            return "경매 처리 배치 수동 실행 완료";
        } catch (Exception e) {
            log.error("[runAuctionProcessingJobManual] 경매 처리 배치 수동 실행 실패", e);
            return "경매 처리 배치 수동 실행 실패: " + e.getMessage();
        }
    }

    @Operation(summary = "재처리 필요한 실패/스킵 로그 조회", description = "배치 처리 중 스킵되어 '실제로 재처리가 가능한' 항목들의 목록을 조회합니다. 기본적으로 SKIP 타입의 로그만 조회합니다.")
    @GetMapping("/failures")
    public ResponseEntity<List<BatchFailureLog>> getReprocessableFailures(
            @Parameter(name = "jobName", description = "조회할 특정 Job의 이름 (선택 사항)", example = "restrictionReleaseJob")
            @RequestParam(required = false) String jobName) {

        // [개선점] 재처리가 의미 있는 FailureType만 기본으로 조회하도록 설정
        // 현재는 SKIP만 재처리 대상입니다.
        Set<BatchFailureLog.FailureType> reprocessableTypes = Set.of(BatchFailureLog.FailureType.SKIP);

        // reprocessed가 false이고, 타입이 재처리 대상인 로그만 조회
        List<BatchFailureLog> failures = batchFailureLogService.getFailures(jobName, null, reprocessableTypes, false);
        return ResponseEntity.ok(failures);
    }

    @Operation(summary = "모든 유형의 실패/스킵 로그 조회", description = "재처리 가능 여부와 상관없이 모든 유형(RETRY_EXHAUSTED 포함)의 실패/스킵 로그를 조회합니다. (디버깅용)")
    @GetMapping("/failures/all")
    public ResponseEntity<List<BatchFailureLog>> getAllFailures(
            @Parameter(name = "jobName", description = "조회할 특정 Job의 이름 (선택 사항)", example = "restrictionReleaseJob")
            @RequestParam(required = false) String jobName,
            @Parameter(name = "reprocessed", description = "재처리 완료 여부 필터링 (선택 사항)", example = "false")
            @RequestParam(required = false) Boolean reprocessed) {

        // 모든 타입의 로그를 조회 (types 파라미터를 null로 전달)
        List<BatchFailureLog> failures = batchFailureLogService.getFailures(jobName, null, null, reprocessed);
        return ResponseEntity.ok(failures);
    }

    @Operation(summary = "실패한 월 데이터 배치 재시작", description = "가장 최근에 실패한 'resetUserDataJob'을 찾아 재시작합니다.")
    @PostMapping("/restart-failed")
    public String restartFailedResetUserDataJob() {
        String jobName = "resetUserDataJob";
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 10);

        for (JobInstance instance : jobInstances) {
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

    @Operation(summary = "단일 실패/스킵 항목 재처리", description = "특정 실패 로그 ID를 사용하여 해당 항목을 재처리합니다.")
    @PostMapping("/failures/{failureLogId}/reprocess")
    public ResponseEntity<String> reprocessSingleFailure(
            @Parameter(name = "failureLogId", description = "재처리할 실패 로그의 ID", required = true, example = "5")
            @PathVariable Long failureLogId) {
        try {
            batchFailureLogService.reprocessFailedItem(failureLogId);
            return ResponseEntity.ok(String.format("실패 로그 ID %d 항목 재처리 성공", failureLogId));
        } catch (Exception e) {
            log.error("실패 로그 ID {} 항목 재처리 실패: {}", failureLogId, e.getMessage());
            return ResponseEntity.internalServerError().body("재처리 실패: " + e.getMessage());
        }
    }

    @Operation(summary = "특정 Job의 실패한 실행(Execution) 목록 조회", description = "Job 이름을 기준으로, 상태가 'FAILED'인 Job 실행(Execution) 기록 목록을 조회합니다.")
    @GetMapping("/{jobName}/executions/failed")
    public ResponseEntity<List<Map<String, Object>>> getFailedExecutions(
            @Parameter(name = "jobName", description = "조회할 Job의 이름", required = true, example = "resetUserDataJob")
            @PathVariable String jobName) {
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

    @Operation(summary = "특정 Job Execution 재시작", description = "실패한 Job 실행(Execution)의 ID를 사용하여 해당 Job을 마지막으로 실패한 지점부터 재시작합니다.")
    @PostMapping("/executions/{executionId}/restart")
    public ResponseEntity<String> restartFailedJob(
            @Parameter(name = "executionId", description = "재시작할 Job Execution의 ID", required = true, example = "123")
            @PathVariable Long executionId) {
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