package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.component.UserDataItemReader;
import eureca.capstone.project.batch.component.UserDataItemWriter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final UserDataItemReader userDataItemReader;
    private final UserDataItemWriter userDataItemWriter;

    @Bean
    public Job resetUserDataJob() {
        return new JobBuilder("resetUserDataJob", jobRepository)
                .start(resetUserDataStep())
                .build();
    }

    @Bean
    public Step resetUserDataStep() {
        return new StepBuilder("resetUserDataStep", jobRepository)
                .<Long, Long>chunk(100, platformTransactionManager)
                .reader(userDataItemReader)
                .writer(userDataItemWriter)
                .faultTolerant()
                .retry(Exception.class)
                .retryLimit(3)
                .build();
    }
}
