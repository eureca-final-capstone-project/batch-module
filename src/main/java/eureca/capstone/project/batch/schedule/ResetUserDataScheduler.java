package eureca.capstone.project.batch.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResetUserDataScheduler {

    private final JobLauncher jobLauncher;
    private final Job resetUserDataJob;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runResetDataBatch() {
        try {
            log.info("[runResetDataBatch] 사용자 데이터 초기화 배치 실행");
            jobLauncher.run(resetUserDataJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters());
            log.info("[runResetDataBatch] 사용자 데이터 초기화 배치 실행 완료");
        } catch (Exception e) {
            log.error("[runResetDataBatch] 사용자 데이터 초기화 배치 실행 실패", e);
        }
    }
}

