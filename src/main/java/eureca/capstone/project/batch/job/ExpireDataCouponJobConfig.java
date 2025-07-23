package eureca.capstone.project.batch.job;


import eureca.capstone.project.batch.alarm.dto.AlarmCreationDto;
import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.service.NotificationProducer;
import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.retry.RetryPolicy;
import eureca.capstone.project.batch.transaction_feed.entity.UserDataCoupon;
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
public class ExpireDataCouponJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final ExecutionListener executionListener;
    private final RetryPolicy retryPolicy;
    private final NotificationProducer notificationProducer;

    @Bean
    public Job expireDataCouponJob(Step expireDataCouponStep) {
        return new JobBuilder("expireDataCouponJob", jobRepository)
                .start(expireDataCouponStep)
                .build();
    }

    @Bean
    public Step expireDataCouponStep(
            JpaPagingItemReader<UserDataCoupon> expireDataCouponReader,
            ItemProcessor<UserDataCoupon, UserDataCoupon> expireDataCouponProcessor,
            ItemWriteListener<UserDataCoupon> expireDataCouponNotifyListener) {
        return new StepBuilder("expireDataCouponStep", jobRepository)
                .<UserDataCoupon, UserDataCoupon>chunk(100, platformTransactionManager)
                .reader(expireDataCouponReader)
                .processor(expireDataCouponProcessor)
                .writer(expireDataCouponWriter())
                .faultTolerant()
                .retryPolicy(retryPolicy.createRetryPolicy())
                .backOffPolicy(retryPolicy.createBackoffPolicy())
                .listener(executionListener)
                .listener(expireDataCouponNotifyListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<UserDataCoupon> expireDataCouponReader(
            @Value("#{jobParameters['currentTime']}") String currentTime,
            @Qualifier("getStatuses") Map<String, Status> statusMap) {
        LocalDateTime today = LocalDateTime.parse(currentTime);
        Status expiredStatus = statusMap.get("EXPIRED");
        Status usedStatus = statusMap.get("USED");
        List<Status> excludedStatuses = List.of(expiredStatus, usedStatus);

        Map<String, Object> params = new HashMap<>();
        params.put("today", today);
        params.put("statuses", excludedStatuses);

        String query = "select udc from UserDataCoupon udc where udc.expiresAt <= :today and udc.status not in :statuses";

        return new JpaPagingItemReaderBuilder<UserDataCoupon>()
                .name("expireDataCouponReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }


    @Bean
    public ItemProcessor<UserDataCoupon, UserDataCoupon> expireDataCouponProcessor(
            @Qualifier("getStatuses") Map<String, Status> statusMap) {
            Status expiredStatus = statusMap.get("EXPIRED");

            return coupon ->{
                coupon.changeStatus(expiredStatus);
                return coupon;
            };
    }

    @Bean
    public ItemWriter<UserDataCoupon> expireDataCouponWriter() {

        return new JdbcBatchItemWriterBuilder<UserDataCoupon>()
                .dataSource(dataSource)
                .sql("update user_data_coupon set status_id = :statusId where user_data_coupon_id = :userDataCouponId")
                .beanMapped()
                .assertUpdates(false)
                .build();
    }

    @Bean
    public ItemWriteListener<UserDataCoupon> expireDataCouponNotifyListener(
            NotificationProducer notificationProducer) {

        return new ItemWriteListener<>() {

            @Override
            public void beforeWrite(Chunk<? extends UserDataCoupon> chunk) { }

            @Override
            public void afterWrite(Chunk<? extends UserDataCoupon> chunk) {
                if (chunk == null || chunk.isEmpty()) return;

                List<AlarmCreationDto> dtos = chunk.getItems().stream()
                        .map(c -> AlarmCreationDto.builder()
                                .userId(c.getUser().getUserId())
                                .alarmType("쿠폰 만료")
                                .content("데이터 쿠폰 \"" + c.getDataCoupon().getCouponNumber() + "\"이 만료되었습니다.")
                                .build())
                        .toList();

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        dtos.forEach(notificationProducer::send);
                        log.info("[expireDataCouponNotifyListener] {}건 알림 전송 완료", dtos.size());
                    }
                });
            }

            @Override
            public void onWriteError(Exception ex, Chunk<? extends UserDataCoupon> chunk) {
                log.error("[expireDataCouponNotifyListener] error 발생. size={}",
                        chunk == null ? 0 : chunk.size(), ex);
            }
        };
    }
}
