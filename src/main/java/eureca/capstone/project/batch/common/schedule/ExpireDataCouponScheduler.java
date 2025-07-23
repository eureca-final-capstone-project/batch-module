package eureca.capstone.project.batch.common.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireDataCouponScheduler {

    private final JobLauncher jobLauncher;
    private final Job expireDataCouponJob;

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void runExpireDataCouponBatch() {
        try {
            log.info("[runExpireDataCouponBatch] 데이터 충전권 기간만료 배치 실행");
            jobLauncher.run(expireDataCouponJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("currentTime", LocalDateTime.now().toString())
                    .toJobParameters());
            log.info("[runExpireDataCouponBatch] 데이터 충전권 기간만료 배치 실행 완료");
        } catch (Exception e) {
            log.error("[runExpireDataCouponBatch] 데이터 충전권 기간만료 배치 실행 실패", e);
        }
    }
}

