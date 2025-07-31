package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.service.StatusService;
import eureca.capstone.project.batch.component.listener.CouponExpirationSummaryListener;
import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.retry.RetryPolicy;
import eureca.capstone.project.batch.transaction_feed.entity.UserDataCoupon;
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
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

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
    private final StatusService statusService;

    @Bean
    public Job expireDataCouponJob() {
        return new JobBuilder("expireDataCouponJob", jobRepository)
                .start(expireDataCouponStep())
                .build();
    }

    @Bean
    public Step expireDataCouponStep() {
        return new StepBuilder("expireDataCouponStep", jobRepository)
                .<UserDataCoupon, UserDataCoupon>chunk(100, platformTransactionManager)
                .reader(expireDataCouponReader(null))
                .processor(expireDataCouponProcessor())
                .writer(expireDataCouponWriter())
                .faultTolerant()
                .retryPolicy(retryPolicy.createRetryPolicy())
                .backOffPolicy(retryPolicy.createBackoffPolicy())
                .listener(executionListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<UserDataCoupon> expireDataCouponReader(
            @Value("#{jobParameters['currentTime']}") String currentTime) {
        LocalDateTime today = LocalDateTime.parse(currentTime);
        Status expiredStatus = statusService.getStatus("COUPON", "EXPIRED");
        Status usedStatus = statusService.getStatus("COUPON", "USED");
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
    public ItemProcessor<UserDataCoupon, UserDataCoupon> expireDataCouponProcessor() {
        Status expiredStatus = statusService.getStatus("COUPON", "EXPIRED");
        return coupon -> {
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
}
