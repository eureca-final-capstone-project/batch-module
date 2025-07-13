package eureca.capstone.project.batch.schedule;

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
public class ResetUserDataScheduler {

    private final JobLauncher jobLauncher;
    private final Job resetUserDataJob;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runAlternatingBatch() {
        runDailyBatch(0);
    }

    public void runDailyBatch(long amount) {
        try {
            log.info("batch 시작 (스케줄러)");
            jobLauncher.run(resetUserDataJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addLong("amount", amount)
                    .toJobParameters());
            log.info("배치 완료(스케줄러)");
        } catch (Exception e) {
            log.error("배치 실패", e);
        }
    }
}

