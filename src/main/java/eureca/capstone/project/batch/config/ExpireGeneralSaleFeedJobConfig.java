package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.alarm.service.NotificationService;
import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import eureca.capstone.project.batch.component.listener.CustomRetryListener;
import eureca.capstone.project.batch.component.listener.CustomSkipListener;
import eureca.capstone.project.batch.component.listener.JobCompletionNotificationListener;
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
import java.util.stream.Collectors;

@Configuration
public class ExpireGeneralSaleFeedJobConfig {
    private final EntityManagerFactory entityManagerFactory;
    private final StatusRepository statusRepository;
    private final JobCompletionNotificationListener jobCompletionNotificationListener;
    private final TransactionFeedRepository transactionFeedRepository;
    private final CustomSkipListener customSkipListener;
    private final CustomRetryListener customRetryListener;
    private final TransactionFeedSearchRepository transactionFeedSearchRepository;
    private final NotificationService notificationService;

    private static final int CHUNK_SIZE = 100;

    private final Status expiredStatus;

    public ExpireGeneralSaleFeedJobConfig(EntityManagerFactory entityManagerFactory,
                                          StatusRepository statusRepository,
                                          JobCompletionNotificationListener jobCompletionNotificationListener,
                                          TransactionFeedRepository transactionFeedRepository,
                                          CustomSkipListener customSkipListener,
                                          CustomRetryListener customRetryListener,
                                          TransactionFeedSearchRepository transactionFeedSearchRepository,
                                          NotificationService notificationService) {
        this.entityManagerFactory = entityManagerFactory;
        this.statusRepository = statusRepository;
        this.jobCompletionNotificationListener = jobCompletionNotificationListener;
        this.transactionFeedRepository = transactionFeedRepository;
        this.customSkipListener = customSkipListener;
        this.customRetryListener = customRetryListener;
        this.expiredStatus = statusRepository.findByDomainAndCode("FEED", "EXPIRED").orElseThrow(() -> new IllegalArgumentException("기간만료 상태를 찾을 수 없습니다."));
        this.transactionFeedSearchRepository = transactionFeedSearchRepository;
        this.notificationService = notificationService;
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

                .listener(customSkipListener)
                .listener(customRetryListener)
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
                    .collect(Collectors.toList());

            if (!idsToUpdate.isEmpty()) {
                transactionFeedRepository.updateStatusForIds(this.expiredStatus, idsToUpdate);
                List<TransactionFeed> updatedFeeds = transactionFeedRepository.findAllById(idsToUpdate);
                List<TransactionFeedDocument> documentsToUpdate = updatedFeeds.stream()
                        .map(TransactionFeedDocument::fromEntity)
                        .collect(Collectors.toList());
                transactionFeedSearchRepository.saveAll(documentsToUpdate);

                updatedFeeds.forEach(feed -> {
                    if (feed.getUser() != null) {
                        notificationService.sendNotification(
                                feed.getUser().getUserId(),
                                "게시글 만료",
                                String.format("[%s] 게시글이 만료되었습니다.", feed.getTitle())
                        );
                    }
                });
            }
        };
    }
}
