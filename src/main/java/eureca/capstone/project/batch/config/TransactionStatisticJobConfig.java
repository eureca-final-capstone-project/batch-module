package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.retry.RetryPolicy;
import eureca.capstone.project.batch.component.tasklet.TransactionStatisticSaveTasklet;
import eureca.capstone.project.batch.component.writer.TransactionStatisticWriter;
import eureca.capstone.project.batch.transaction_feed.entity.DataTransactionHistory;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.sql.SQLRecoverableException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransactionStatisticJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final RetryPolicy retryPolicy;
    private final ExecutionListener executionListener;
    private final TransactionStatisticWriter transactionStatisticWriter;
    private final TransactionStatisticSaveTasklet transactionStatisticSaveTasklet;

    @Bean
    public Job transactionStatisticJob() {
        return new JobBuilder("transactionStatisticJob", jobRepository)
                .start(transactionStatisticCalculateStep()) // 거래내역 조회 및 누적 집계
                .next(transactionStatisticSaveStep())       // 통계 계산 및 저장
                .build();
    }

    @Bean
    public Step transactionStatisticCalculateStep() {
        return new StepBuilder("transactionStatisticCalculateStep", jobRepository)
                .<DataTransactionHistory, DataTransactionHistory>chunk(100, platformTransactionManager)
                .reader(transactionHistoryJpaReader(null))
                .writer(transactionStatisticWriter)
                .faultTolerant()
                .skip(DataIntegrityViolationException.class)
                .skipLimit(3)
                .retryPolicy(retryPolicy.createRetryPolicy())
                .backOffPolicy(retryPolicy.createBackoffPolicy())
                .listener(executionListener)
                .listener(promotionListener())
                .build();
    }

    @Bean
    public Step transactionStatisticSaveStep() {
        return new StepBuilder("transactionStatisticSaveStep", jobRepository)
                .tasklet(transactionStatisticSaveTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    public ExecutionContextPromotionListener promotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {TransactionStatisticWriter.STEP_STATISTIC_KEY});
        return listener;
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<DataTransactionHistory> transactionHistoryJpaReader(
            @Value("#{jobParameters['currentTime']}") String currentDate) {
        LocalDateTime currentTime = LocalDateTime.parse(currentDate);
        LocalDateTime startTime = currentTime
                .minusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime endTime = currentTime
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Map<String, Object> params = new HashMap<>();
        params.put("start", startTime);
        params.put("end", endTime);
        params.put("typeId", 1);



        String query = "select th from DataTransactionHistory th " +
                "join fetch th.transactionFeed tf " +
                "join fetch tf.telecomCompany tc " +
                "join fetch tf.salesType st " +
                "where th.createdAt >= :start and th.createdAt < :end and st.salesTypeId = :typeId";

        return new JpaPagingItemReaderBuilder<DataTransactionHistory>()
                .name("transactionHistoryJpaReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }

    @Bean
    public SimpleRetryPolicy retryPolicyStatistic(){
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(IOException.class, true);
        retryableExceptions.put(TimeoutException.class, true);
        retryableExceptions.put(SQLRecoverableException.class, true);
        retryableExceptions.put(TransientDataAccessException.class, true);

        return new SimpleRetryPolicy(3, retryableExceptions);
    }

    @Bean
    public FixedBackOffPolicy fixedBackOffPolicyStatistic(){
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(2000);
        return fixedBackOffPolicy;
    }
}
