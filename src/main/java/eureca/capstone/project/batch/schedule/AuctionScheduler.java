package eureca.capstone.project.batch.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuctionScheduler {

    private final JobLauncher jobLauncher;
    private final Job auctionProcessingJob;

    @Scheduled(cron = "0 0 0 * * *") // 매 1분마다 실행
    public void runAuctionProcessingJob() {
        log.info("[AuctionScheduler] 경매 처리 배치 시작");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("targetDateTime", LocalDateTime.now())
                    .addLong("salesTypeId", 2L)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(auctionProcessingJob, jobParameters);
            log.info("[AuctionScheduler] 경매 처리 배치 종료");
        } catch (Exception e) {
            log.error("[AuctionScheduler] 경매 처리 배치 실행 중 오류 발생", e);
        }
    }
}
