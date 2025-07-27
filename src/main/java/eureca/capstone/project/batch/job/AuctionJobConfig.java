package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.alarm.service.NotificationService;
import eureca.capstone.project.batch.auction.service.AuctionBatchService;
import eureca.capstone.project.batch.component.listener.CustomRetryListener;
import eureca.capstone.project.batch.component.listener.CustomSkipListener;
import eureca.capstone.project.batch.component.listener.JobCompletionNotificationListener;
import eureca.capstone.project.batch.transaction_feed.document.TransactionFeedDocument;
import eureca.capstone.project.batch.transaction_feed.entity.Bids;
import eureca.capstone.project.batch.transaction_feed.entity.TransactionFeed;
import eureca.capstone.project.batch.transaction_feed.repository.BidsRepository;
import eureca.capstone.project.batch.transaction_feed.repository.TransactionFeedSearchRepository;
import eureca.capstone.project.batch.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuctionJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final AuctionBatchService auctionBatchService;
    private final BidsRepository bidsRepository;
    private final JobCompletionNotificationListener jobCompletionNotificationListener;
    private final CustomSkipListener customSkipListener;
    private final CustomRetryListener customRetryListener;
    private final TransactionFeedSearchRepository transactionFeedSearchRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job auctionProcessingJob() {
        return new JobBuilder("auctionProcessingJob", jobRepository)
                .listener(jobCompletionNotificationListener) // Job 리스너 등록
                .start(auctionProcessingStep())
                .build();
    }

    @Bean
    public Step auctionProcessingStep() {
        return new StepBuilder("auctionProcessingStep", jobRepository)
                .<TransactionFeed, AuctionResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(auctionFeedReader(null, null))
                .processor(auctionFeedProcessor())
                .writer(auctionFeedWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(OptimisticLockingFailureException.class) // 재시도할 예외
                .skipLimit(10)
                .skip(RuntimeException.class)
                .listener(customSkipListener)
                .listener(customRetryListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<TransactionFeed> auctionFeedReader(
            @Value("#{jobParameters['targetDateTime']}") LocalDateTime targetDateTime,
            @Value("#{jobParameters['salesTypeId']}") Long salesTypeId) {

        String jpqlQuery = """
            SELECT tf FROM TransactionFeed tf
            JOIN FETCH tf.status
            JOIN FETCH tf.salesType
            JOIN FETCH tf.user
            JOIN FETCH tf.telecomCompany
            WHERE tf.salesType.salesTypeId = :salesTypeId
            AND tf.status.code = 'ON_SALE'
            AND tf.isDeleted = false
            AND tf.expiresAt < :targetDateTime
            """;

        JpaPagingItemReader<TransactionFeed> reader = new JpaPagingItemReaderBuilder<TransactionFeed>()
                .name("auctionFeedReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(jpqlQuery)
                .parameterValues(Map.of(
                        "salesTypeId", salesTypeId,
                        "targetDateTime", targetDateTime
                ))
                .build();

        // 이 reader가 자체적인 트랜잭션을 사용하지 않도록 설정합니다.
        reader.setTransacted(false);

        return reader;
    }

    @Bean
    public ItemProcessor<TransactionFeed, AuctionResult> auctionFeedProcessor() {
        return feed -> {
            log.info("[AuctionJobConfig] 판매글 처리 시작: ID = {}", feed.getTransactionFeedId());

            List<Bids> bids = bidsRepository.findHighestBidWithUser(feed.getTransactionFeedId(), PageRequest.of(0,1));
            Optional<Bids> highestBid = bids.isEmpty() ? Optional.empty() : Optional.of(bids.get(0));

            if (highestBid.isPresent()) {
                Bids bid = highestBid.get();
                Long finalBidAmount = bid.getBidAmount();
                User buyer = bid.getUser();
                log.info("[AuctionJobConfig] 낙찰자 발견: ID = {}, 금액 = {}", buyer.getUserId(), finalBidAmount);
                return new AuctionResult(feed, buyer, finalBidAmount, AuctionResult.Type.WINNING);
            } else {
                // 유찰 처리
                log.info("[AuctionJobConfig] 유찰 처리: ID = {}", feed.getTransactionFeedId());
                return new AuctionResult(feed, null, null, AuctionResult.Type.FAILED);
            }
        };
    }

    @Bean
    public ItemWriter<AuctionResult> auctionFeedWriter() {
        return chunk -> {
            for (AuctionResult result : chunk.getItems()) {
                TransactionFeed managedFeed = entityManager.merge(result.getTransactionFeed());
                if (result.getType() == AuctionResult.Type.WINNING) {
                    auctionBatchService.processWinningBid(managedFeed, result.getBuyer(), result.getFinalBidAmount());
                    // 낙찰자에게 알림 전송
                    if (result.getBuyer() != null) {
                        notificationService.sendNotification(
                                result.getBuyer().getUserId(),
                                "구매",
                                String.format("[%s] 게시글에 낙찰되었습니다! 낙찰 금액: %d", managedFeed.getTitle(), result.getFinalBidAmount())
                        );
                    }
                    // 판매자에게 알림 전송
                    if (managedFeed.getUser() != null) {
                        notificationService.sendNotification(
                                managedFeed.getUser().getUserId(),
                                "판매",
                                String.format("[%s] 게시글이 낙찰되었습니다! 낙찰 금액: %d", managedFeed.getTitle(), result.getFinalBidAmount())
                        );
                    }
                } else if (result.getType() == AuctionResult.Type.FAILED) {
                    auctionBatchService.processFailedBid(managedFeed);
                    // 유찰된 판매글 등록자에게 알림 전송
                    if (managedFeed.getUser() != null) {
                        notificationService.sendNotification(
                                managedFeed.getUser().getUserId(),
                                "게시글 만료",
                                String.format("[%s] 게시글이 유찰되었습니다.", managedFeed.getTitle())
                        );
                    }
                }

                transactionFeedSearchRepository.save(TransactionFeedDocument.fromEntity(managedFeed));
            }
        };
    }


    public static class AuctionResult {
        enum Type { WINNING, FAILED }

        private final TransactionFeed transactionFeed;
        private final User buyer;
        private final Long finalBidAmount;
        private final Type type;

        public AuctionResult(TransactionFeed transactionFeed, User buyer, Long finalBidAmount, Type type) {
            this.transactionFeed = transactionFeed;
            this.buyer = buyer;
            this.finalBidAmount = finalBidAmount;
            this.type = type;
        }

        public TransactionFeed getTransactionFeed() { return transactionFeed; }
        public User getBuyer() { return buyer; }
        public Long getFinalBidAmount() { return finalBidAmount; }
        public Type getType() { return type; }
    }
}
