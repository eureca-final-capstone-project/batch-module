package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.retry.RetryPolicy;
import eureca.capstone.project.batch.component.tasklet.BidStatisticSaveTasklet;
import eureca.capstone.project.batch.component.tasklet.NormalStatisticSaveTasklet;
import eureca.capstone.project.batch.component.writer.NormalStatisticWriter;
import eureca.capstone.project.batch.component.writer.BidVolumeStatisticWriter;
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
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransactionStatisticJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final RetryPolicy retryPolicy;
    private final ExecutionListener executionListener;
    private final NormalStatisticWriter normalStatisticWriter;
    private final BidVolumeStatisticWriter bidVolumeStatisticWriter;
    private final NormalStatisticSaveTasklet normalStatisticSaveTasklet;
    private final BidStatisticSaveTasklet bidStatisticSaveTasklet;

    // 일반판매 거래량 + 시세 통계 JOB
    @Bean
    public Job normalStatisticJob() {
        return new JobBuilder("normalStatisticJob", jobRepository)
                .start(normalStatisticCalculateStep()) // 시세 통계 조회 및 누적 집계
                .next(normalStatisticSaveStep())       // 통계 계산 및 저장
                .build();
    }

    // 입찰판매 거래량 통계 JOB
    @Bean
    public Job bidStatisticJob() {
        return new JobBuilder("bidStatisticJob", jobRepository)
                .start(bidStatisticCalculateStep()) // 시세 통계 조회 및 누적 집계
                .next(bidStatisticSaveStep())       // 통계 계산 및 저장
                .build();
    }

    // 시세통계 + 일반판매 거래량 집계 STEP [normalStatisticJob]
    @Bean
    public Step normalStatisticCalculateStep() {
        return new StepBuilder("normalStatisticCalculateStep", jobRepository)
                .<DataTransactionHistory, DataTransactionHistory>chunk(100, platformTransactionManager)
                .reader(transactionHistoryJpaReader(null))
                .writer(normalStatisticWriter)
                .faultTolerant()
                .skip(DataIntegrityViolationException.class)
                .skipLimit(3)
                .retryPolicy(retryPolicy.createRetryPolicy())
                .backOffPolicy(retryPolicy.createBackoffPolicy())
                .listener(executionListener)
                .listener(promotionListener())
                .build();
    }

    // 시세통계 + 일반판매 거래량 저장 STEP [normalStatisticJob]
    @Bean
    public Step normalStatisticSaveStep() {
        return new StepBuilder("normalStatisticSaveStep", jobRepository)
                .tasklet(normalStatisticSaveTasklet, platformTransactionManager)
                .build();
    }

    // 입찰판매 거래량 집계 STEP [bidStatisticJob]
    @Bean
    public Step bidStatisticCalculateStep() {
        return new StepBuilder("bidStatisticCalculateStep", jobRepository)
                .<DataTransactionHistory, DataTransactionHistory>chunk(100, platformTransactionManager)
                .reader(volumeJpaReader(null))
                .writer(bidVolumeStatisticWriter)
                .listener(promotionListener())
                .build();
    }

    // 입찰판매 거래량 저장 STEP [bidStatisticJob]
    @Bean
    public Step bidStatisticSaveStep() {
        return new StepBuilder("bidStatisticSaveStep", jobRepository)
                .tasklet(bidStatisticSaveTasklet, platformTransactionManager)
                .build();
    }


    // 시세통계 + 일반판매 거래량 READER [normalStatisticJob]
    @Bean
    @StepScope
    public JpaPagingItemReader<DataTransactionHistory> transactionHistoryJpaReader(
            @Value("#{jobParameters['currentTime']}") String currentDate) {
        LocalDateTime currentTime = LocalDateTime.parse(currentDate);
        LocalDateTime from = currentTime
                .minusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime to = currentTime
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Map<String,Object> params = Map.of(
                "start", from,
                "end",   to,
                "typeId", 1
        );

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

    // 입찰판매 거래량 READER [bidStatisticJob]
    @Bean
    @StepScope
    public JpaPagingItemReader<DataTransactionHistory> volumeJpaReader(
            @Value("#{jobParameters['currentTime']}") String currentTime) {

        LocalDate today = LocalDateTime.parse(currentTime).toLocalDate();
        LocalDateTime from  = today.atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        log.info("{} ~ {}", from, to);
        Map<String,Object> params = Map.of(
                "start", from,
                "end",   to,
                "typeId", 2
        );

        String query = "select th from DataTransactionHistory th " +
                "join fetch th.transactionFeed tf " +
                "join fetch tf.salesType st " +
                "where th.createdAt >= :start and th.createdAt < :end and st.salesTypeId = :typeId";

        return new JpaPagingItemReaderBuilder<DataTransactionHistory>()
                .name("volumeJpaReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }

    @Bean
    public ExecutionContextPromotionListener promotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {
                NormalStatisticWriter.NORMAL_STATISTIC_KEY,
                BidVolumeStatisticWriter.VOLUME_STATISTIC_KEY
        });
        return listener;
    }
}
