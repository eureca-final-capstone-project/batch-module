package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.alarm.dto.AlarmCreationDto;
import eureca.capstone.project.batch.alarm.service.NotificationService;
import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.retry.RetryPolicy;
import eureca.capstone.project.batch.pay.entity.UserEventCoupon;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ExpireEventCouponJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final ExecutionListener executionListener;
    private final RetryPolicy retryPolicy;

    @Bean
    public Job expireEventCouponJob(Step expireEventCouponStep) {
        return new JobBuilder("expireEventCouponJob", jobRepository)
                .start(expireEventCouponStep)
                .build();
    }

    @Bean
    public Step expireEventCouponStep(
            JpaPagingItemReader<UserEventCoupon> expireEventCouponReader,
            ItemProcessor<UserEventCoupon, UserEventCoupon> expireEventCouponProcessor,
            ItemWriteListener<UserEventCoupon> expireEventCouponNotifyListener) {
        return new StepBuilder("expireEventCouponStep", jobRepository)
                .<UserEventCoupon, UserEventCoupon>chunk(100, platformTransactionManager)
                .reader(expireEventCouponReader)
                .processor(expireEventCouponProcessor)
                .writer(expireEventCouponWriter())
                .faultTolerant()
                .retryPolicy(retryPolicy.createRetryPolicy())
                .backOffPolicy(retryPolicy.createBackoffPolicy())
                .listener(executionListener)
                .listener(expireEventCouponNotifyListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<UserEventCoupon> expireEventCouponReader(
            @Value("#{jobParameters['currentTime']}") String currentTime,
            @Qualifier("getStatuses") Map<String, Status> statusMap) {
        LocalDateTime today = LocalDateTime.parse(currentTime);
        Status expiredStatus = statusMap.get("EXPIRED");
        Status usedStatus = statusMap.get("USED");
        List<Status> excludedStatuses = List.of(expiredStatus, usedStatus);

        Map<String, Object> params = new HashMap<>();
        params.put("today", today);
        params.put("statuses", excludedStatuses);

        String query = "select uec from UserEventCoupon uec where uec.expiresAt <= :today and uec.status not in :statuses";

        return new JpaPagingItemReaderBuilder<UserEventCoupon>()
                .name("expireEventCouponReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }


    @Bean
    public ItemProcessor<UserEventCoupon, UserEventCoupon> expireEventCouponProcessor(
            @Qualifier("getStatuses") Map<String, Status> statusMap) {
            Status expiredStatus = statusMap.get("EXPIRED");

            return coupon ->{
                coupon.changeStatus(expiredStatus);
                return coupon;
            };
    }

    @Bean
    public ItemWriter<UserEventCoupon> expireEventCouponWriter() {

        return new JdbcBatchItemWriterBuilder<UserEventCoupon>()
                .dataSource(dataSource)
                .sql("update user_event_coupon set status_id = :statusId where user_event_coupon_id = :userEventCouponId")
                .beanMapped()
                .assertUpdates(false)
                .build();
    }

    @Bean
    public ItemWriteListener<UserEventCoupon> expireEventCouponNotifyListener(
            NotificationService notificationService) {

        return new ItemWriteListener<>() {

            @Override
            public void beforeWrite(Chunk<? extends UserEventCoupon> chunk) { }

            @Override
            public void afterWrite(Chunk<? extends UserEventCoupon> chunk) {
                if (chunk == null || chunk.isEmpty()) return;

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        chunk.getItems().forEach(c -> {
                            notificationService.sendNotification(
                                    c.getUser().getUserId(),
                                    "쿠폰 만료",
                                    "이벤트 쿠폰 \"" + c.getEventCoupon().getCouponName() + "\"이(가) 만료되었습니다."
                            );
                        });

                        log.info("[expireEventCouponNotifyListener] {}건 알림 전송 완료", chunk.getItems().size());
                    }
                });
            }

            @Override
            public void onWriteError(Exception ex, Chunk<? extends UserEventCoupon> chunk) {
                log.error("[expireEventCouponNotifyListener] error 발생. size={}",
                        chunk == null ? 0 : chunk.size(), ex);
            }
        };
    }
}
