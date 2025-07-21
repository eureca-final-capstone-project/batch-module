package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.component.listener.UserDataListener;
import eureca.capstone.project.batch.component.UserDataProcessor;
import eureca.capstone.project.batch.user.entity.UserData;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLRecoverableException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final UserDataProcessor userDataProcessor;
    private final DataSource dataSource;
    private final UserDataListener userDataListener;

    @Bean
    public Job resetUserDataJob() {
        return new JobBuilder("resetUserDataJob", jobRepository)
                .start(resetUserDataStep())
                .build();
    }

    @Bean
    public Step resetUserDataStep() {
        return new StepBuilder("resetUserDataStep", jobRepository)
                .<UserData, UserData>chunk(100, platformTransactionManager)
                .reader(userDataJpaReader(null))
                .processor(userDataProcessor)
                .writer(userDataWriter())
                .faultTolerant()
                .retryPolicy(retryPolicy())
                .backOffPolicy(fixedBackOffPolicy())
                .listener(userDataListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<UserData> userDataJpaReader(
            @Value("#{jobParameters['currentDate']}") String currentDate) {
        LocalDate date = LocalDate.parse(currentDate);
        Integer today = date.getDayOfMonth()-2;
        Integer lastDay = date.lengthOfMonth();
        Map<String, Object> params = new HashMap<>();
        params.put("today", today);

        String query;
        if(today.equals(lastDay)) {
            query = "select ud from UserData ud join fetch ud.plan where ud.resetDataAt >= :today";
        } else{
            query = "select ud from UserData ud join fetch ud.plan where ud.resetDataAt = :today";
        }

        return new JpaPagingItemReaderBuilder<UserData>()
                .name("userJpaReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemWriter<UserData> userDataWriter() {
        return new JdbcBatchItemWriterBuilder<UserData>()
                .dataSource(dataSource)
                .sql("update user_data set sellable_data_mb = :sellableDataMb, total_data_mb =:totalDataMb where user_data_id = :userDataId")
                .beanMapped()
                .assertUpdates(false)
                .build();
    }

    @Bean
    public SimpleRetryPolicy retryPolicy(){
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(IOException.class, true);
        retryableExceptions.put(TimeoutException.class, true);
        retryableExceptions.put(SQLRecoverableException.class, true);
        retryableExceptions.put(TransientDataAccessException.class, true);

        return new SimpleRetryPolicy(3, retryableExceptions);
    }

    @Bean
    public FixedBackOffPolicy fixedBackOffPolicy(){
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(2000);
        return fixedBackOffPolicy;
    }


}
