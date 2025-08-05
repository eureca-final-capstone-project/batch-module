package eureca.capstone.project.batch.common.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordBatchScheduler {
    private final JobLauncher jobLauncher;
    private final Job keywordRankingJob;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void runKeywordRankingJob() {
        log.info("[KeywordBatchScheduler] 실시간 검색어 키워드 공급 배치 시작");
        try{
            JobParameters params = new JobParametersBuilder()
                    .addString("JobID", String.valueOf(System.currentTimeMillis()))
                    .toJobParameters();
            jobLauncher.run(keywordRankingJob, params);
        } catch (Exception e){
            log.error("[KeywordBatchScheduler] 배치 실행 중 오류 발생", e);
        }
    }
}
