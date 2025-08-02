package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import eureca.capstone.project.batch.component.listener.CustomRetryListener;
import eureca.capstone.project.batch.component.listener.CustomSkipListener;
import eureca.capstone.project.batch.component.listener.GeneralSaleFeedNotificationListener;
//import eureca.capstone.project.batch.component.listener.JobCompletionNotificationListener;
import eureca.capstone.project.batch.transaction_feed.document.TransactionFeedDocument;
import eureca.capstone.project.batch.transaction_feed.entity.TransactionFeed;
import eureca.capstone.project.batch.transaction_feed.repository.TransactionFeedRepository;
import eureca.capstone.project.batch.transaction_feed.repository.TransactionFeedSearchRepository;
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

@Configuration
public class ExpireGeneralSaleFeedJobConfig {
    private final EntityManagerFactory entityManagerFactory;
//    private final JobCompletionNotificationListener jobCompletionNotificationListener;
    private final TransactionFeedRepository transactionFeedRepository;
    private final CustomSkipListener customSkipListener;
    private final CustomRetryListener customRetryListener;
    private final TransactionFeedSearchRepository transactionFeedSearchRepository;
    private final GeneralSaleFeedNotificationListener generalSaleFeedNotificationListener;

    private static final int CHUNK_SIZE = 100;

    private final Status expiredStatus;

    public ExpireGeneralSaleFeedJobConfig(EntityManagerFactory entityManagerFactory,
                                          StatusRepository statusRepository,
//                                          JobCompletionNotificationListener jobCompletionNotificationListener,
                                          TransactionFeedRepository transactionFeedRepository,
                                          CustomSkipListener customSkipListener,
                                          CustomRetryListener customRetryListener,
                                          TransactionFeedSearchRepository transactionFeedSearchRepository,
                                          GeneralSaleFeedNotificationListener generalSaleFeedNotificationListener) {
        this.entityManagerFactory = entityManagerFactory;
//        this.jobCompletionNotificationListener = jobCompletionNotificationListener;
        this.transactionFeedRepository = transactionFeedRepository;
        this.customSkipListener = customSkipListener;
        this.customRetryListener = customRetryListener;
        this.expiredStatus = statusRepository.findByDomainAndCode("FEED", "EXPIRED").orElseThrow(() -> new IllegalArgumentException("기간만료 상태를 찾을 수 없습니다."));
        this.transactionFeedSearchRepository = transactionFeedSearchRepository;
        this.generalSaleFeedNotificationListener = generalSaleFeedNotificationListener;
    }

    @Bean
    public Job expireGeneralSaleFeedJob(JobRepository jobRepository, Step expireGeneralSaleFeedStep) {
        return new JobBuilder("expireGeneralSaleFeedJob", jobRepository)
//                .listener(jobCompletionNotificationListener)
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

                .listener(customSkipListener)
                .listener(customRetryListener)
                .listener(generalSaleFeedNotificationListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<TransactionFeed> expireGeneralSaleFeedReader(
            @Value("#{jobParameters['targetDateTime']}") LocalDateTime targetDateTime) {


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
                    .toList();

            if (!idsToUpdate.isEmpty()) {
                transactionFeedRepository.updateStatusForIds(this.expiredStatus, idsToUpdate);
                List<TransactionFeed> updatedFeeds = transactionFeedRepository.findAllById(idsToUpdate);
                List<TransactionFeedDocument> documentsToUpdate = updatedFeeds.stream()
                        .map(TransactionFeedDocument::fromEntity)
                        .toList();
                transactionFeedSearchRepository.saveAll(documentsToUpdate);
            }
        };
    }
}
