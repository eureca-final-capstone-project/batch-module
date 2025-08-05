package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.component.tasklet.KeywordExtractionTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class KeywordRankingJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final KeywordExtractionTasklet keywordExtractionTasklet;

    @Bean
    public Job keywordRankingJob() {
        return new JobBuilder("keywordRankingJob", jobRepository)
                .start(keywordExtractionStep())
                .build();
    }

    @Bean
    public Step keywordExtractionStep() {
        return new StepBuilder("keywordExtractionStep", jobRepository)
                .tasklet(keywordExtractionTasklet, platformTransactionManager)
                .build();
    }
}
