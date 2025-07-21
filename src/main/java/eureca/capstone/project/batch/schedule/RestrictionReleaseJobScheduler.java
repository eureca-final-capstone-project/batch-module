package eureca.capstone.project.batch.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class RestrictionReleaseJobScheduler {
    private final JobLauncher jobLauncher;
    private final Job restrictionReleaseJob;

    public RestrictionReleaseJobScheduler(JobLauncher jobLauncher, @Qualifier("restrictionReleaseJob") Job restrictionReleaseJob) {
        this.jobLauncher = jobLauncher;
        this.restrictionReleaseJob = restrictionReleaseJob;
    }

    @Scheduled(cron = "40 0 0 * * *")
    public void runRestrictionReleaseJob() throws Exception {
        log.info("사용자 제재 만료 처리 배치 작업을 시작합니다.");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("now", LocalDateTime.now())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(restrictionReleaseJob, jobParameters);

            log.info("사용자 제재 만료 처리 배치 작업을 성공적으로 완료했습니다.");

        } catch (Exception e) {
            log.error("사용자 제재 만료 처리 배치 작업 실행 중 오류가 발생했습니다.", e);
        }
    }
}
