package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import eureca.capstone.project.batch.component.listener.CustomSkipListener;
import eureca.capstone.project.batch.component.listener.JobCompletionNotificationListener;
import eureca.capstone.project.batch.transaction_feed.domain.TransactionFeed;
import eureca.capstone.project.batch.transaction_feed.repository.TransactionFeedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class ExpireGeneralSaleFeedJobConfig {
    private final EntityManagerFactory entityManagerFactory;
    private final StatusRepository statusRepository;
    private final JobCompletionNotificationListener jobCompletionNotificationListener;
    private final TransactionFeedRepository transactionFeedRepository;
    private final CustomSkipListener customSkipListener;
    private static final int CHUNK_SIZE = 100;

    private final Status expiredStatus;

    public ExpireGeneralSaleFeedJobConfig(EntityManagerFactory entityManagerFactory,
                                          StatusRepository statusRepository,
                                          JobCompletionNotificationListener jobCompletionNotificationListener,
                                          TransactionFeedRepository transactionFeedRepository,
                                          CustomSkipListener customSkipListener) {
        this.entityManagerFactory = entityManagerFactory;
        this.statusRepository = statusRepository;
        this.jobCompletionNotificationListener = jobCompletionNotificationListener;
        this.transactionFeedRepository = transactionFeedRepository;
        this.customSkipListener = customSkipListener;
        this.expiredStatus = statusRepository.findByDomainAndCode("FEED", "EXPIRED").orElseThrow(() -> new IllegalArgumentException("기간만료 상태를 찾을 수 없습니다."));
    }

    @Bean
    public Job expireGeneralSaleFeedJob(JobRepository jobRepository, Step expireGeneralSaleFeedStep) {
        return new JobBuilder("expireGeneralSaleFeedJob", jobRepository)
                .listener(jobCompletionNotificationListener)
                .start(expireGeneralSaleFeedStep)
                .build();
    }

    @Bean
    public Step expireGeneralSaleFeedStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("expireGeneralSaleFeedStep", jobRepository)
                .<TransactionFeed, TransactionFeed>chunk(CHUNK_SIZE, transactionManager)
                .reader(expireGeneralSaleFeedReader(null))
                .processor(expireGeneralSaleFeedProcessor())
                .writer(customItemWriter())
                .faultTolerant()

                .retryLimit(3)
                .retry(OptimisticLockingFailureException.class)

                .skipLimit(10)
                .skip(NullPointerException.class)
                .skip(IllegalArgumentException.class)

                // Skip 발생 시 로그를 남기기 위한 리스너 등록
                .listener(customSkipListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<TransactionFeed> expireGeneralSaleFeedReader(
            @Value("#{jobParameters['targetDateTime']}") String targetDateTimeStr) {

        LocalDateTime targetDateTime = LocalDateTime.parse(targetDateTimeStr);

        String jpqlQuery = """
                SELECT tf FROM TransactionFeed tf
                JOIN FETCH tf.status
                JOIN FETCH tf.salesType
                WHERE tf.salesType.name ='일반 판매'
                AND tf.status.code = 'ON_SALE'
                AND tf.isDeleted = false
                AND tf.expiresAt < :targetDateTime
                """;

        return new JpaPagingItemReaderBuilder<TransactionFeed>()
                .name("expireGeneralSaleFeedReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(jpqlQuery)
                .parameterValues(Map.of("targetDateTime", targetDateTime))
                .build();
    }

    @Bean
    public ItemProcessor<TransactionFeed, TransactionFeed> expireGeneralSaleFeedProcessor() {
        return feed -> feed;
    }

    @Bean
    public ItemWriter<TransactionFeed> customItemWriter() {
        return chunk -> {
            List<Long> idsToUpdate = chunk.getItems().stream()
                    .map(TransactionFeed::getTransactionFeedId)
                    .collect(Collectors.toList());

            if (!idsToUpdate.isEmpty()) {
                transactionFeedRepository.updateStatusForIds(this.expiredStatus, idsToUpdate);
            }
        };
    }
}
