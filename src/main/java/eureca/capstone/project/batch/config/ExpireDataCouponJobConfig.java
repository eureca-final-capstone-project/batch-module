package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.processor.UserDataProcessor;
import eureca.capstone.project.batch.component.retry.RetryPolicy;
import eureca.capstone.project.batch.transaction_feed.domain.UserDataCoupon;
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

import javax.sql.DataSource;
import java.time.LocalDate;
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
    private final StatusRepository statusRepository;

    @Bean
    public Job expireDataCouponJob(Step expireDataCouponStep) {
        return new JobBuilder("expireDataCouponJob", jobRepository)
                .start(expireDataCouponStep)
                .build();
    }

    @Bean
    public Step expireDataCouponStep(
            JpaPagingItemReader<UserDataCoupon> expireDataCouponReader,
            ItemProcessor<UserDataCoupon, UserDataCoupon> expireDataCouponProcessor) {
        return new StepBuilder("expireDataCouponStep", jobRepository)
                .<UserDataCoupon, UserDataCoupon>chunk(100, platformTransactionManager)
                .reader(expireDataCouponReader)
                .processor(expireDataCouponProcessor)
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
            @Value("#{jobParameters['currentTime']}") String currentTime,
            @Qualifier("couponStatuses") Map<String, Status> statusMap) {
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
            @Qualifier("couponStatuses") Map<String, Status> statusMap) {
            Status expiredStatus = statusMap.get("EXPIRED");

            return coupon ->{
                coupon.updateStatus(expiredStatus);
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
    public Map<String, Status> couponStatuses() {
        Status expired = statusRepository.findByDomainAndCode("COUPON", "EXPIRED")
                .orElseThrow(() -> new RuntimeException("EXPIRED status not found"));
        log.info("EXPIRED status");
        Status used = statusRepository.findByDomainAndCode("COUPON", "USED")
                .orElseThrow(() -> new RuntimeException("USED status not found"));
        log.info("Used status");
        Map<String, Status> map = new HashMap<>();
        map.put("EXPIRED", expired);
        map.put("USED", used);
        return map;
    }
}
