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
public class ExpireGeneralSaleFeedScheduler {
    private final JobLauncher jobLauncher;
    private final Job expireGeneralSaleFeedJob;

    public ExpireGeneralSaleFeedScheduler(JobLauncher jobLauncher, @Qualifier("expireGeneralSaleFeedJob") Job expireGeneralSaleFeedJob) {
        this.jobLauncher = jobLauncher;
        this.expireGeneralSaleFeedJob = expireGeneralSaleFeedJob;
    }

    @Scheduled(cron = "0 43 * * * *")
    public void runExpireGeneralSaleFeedJob() throws Exception {
        log.info("게시글 기간 만료 배치 시작 (스케줄러)");
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDateTime", LocalDateTime.now().toString())
                .toJobParameters();

        jobLauncher.run(expireGeneralSaleFeedJob, jobParameters);
        log.info("게시글 기간 만료 배치 종료 (스케줄러)");
    }
}
