package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.service.StatusService;
import eureca.capstone.project.batch.component.listener.CouponExpirationSummaryListener;
//import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.listener.CustomRetryListener;
import eureca.capstone.project.batch.component.listener.CustomSkipListener;
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
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final CouponExpirationSummaryListener summaryListener;
    private final CustomSkipListener customSkipListener;
    private final CustomRetryListener customRetryListener;

    @Bean
    public Job expireDataCouponJob(
            Step expireDataCouponStep
    ) {
        return new JobBuilder("expireDataCouponJob", jobRepository)
                .start(expireDataCouponStep)
//                .listener(summaryListener)
                .build();
    }

    @Bean
    public Step expireDataCouponStep(
            JdbcPagingItemReader<Long> expireDataCouponJdbcReader
    ) {
        return new StepBuilder("expireDataCouponStep", jobRepository)
                .<Long, Long>chunk(100, platformTransactionManager)
                .reader(expireDataCouponJdbcReader)
//                .processor(expireDataCouponProcessor())
                .writer(expireDataCouponWriter())
                .faultTolerant()
                .skip(DataIntegrityViolationException.class)
                .skipLimit(3)
                .retryPolicy(retryPolicy.createRetryPolicy())
                .backOffPolicy(retryPolicy.createBackoffPolicy())
                .listener(customSkipListener)
                .listener(customRetryListener)
                .listener(executionListener)
//                .listener(summaryListener)
                .build();
    }

//@Bean
//public Step expireDataCouponStep(
//        JpaPagingItemReader<UserDataCoupon> expireDataCouponReader
//) {
//    return new StepBuilder("expireDataCouponStep", jobRepository)
//            .<UserDataCoupon, UserDataCoupon>chunk(100, platformTransactionManager)
//            .reader(expireDataCouponReader)
////                .processor(expireDataCouponProcessor())
//            .writer(expireDataCouponWriter())
//            .faultTolerant()
//            .skip(DataIntegrityViolationException.class)
//            .skipLimit(3)
//            .retryPolicy(retryPolicy.createRetryPolicy())
//            .backOffPolicy(retryPolicy.createBackoffPolicy())
//            .listener(customSkipListener)
//            .listener(customRetryListener)
//            .listener(executionListener)
////                .listener(summaryListener)
//            .build();
//}
//    @Bean
//    @StepScope
//    public JpaPagingItemReader<UserDataCoupon> expireDataCouponReader(
//            @Value("#{jobParameters['currentTime']}") String currentTime) {
//        LocalDateTime today = LocalDateTime.parse(currentTime);
//        Status expiredStatus = statusService.getStatus("COUPON", "EXPIRED");
//        Status usedStatus = statusService.getStatus("COUPON", "USED");
//        List<Status> excludedStatuses = List.of(expiredStatus, usedStatus);
//
//        Map<String, Object> params = new HashMap<>();
//        params.put("today", today);
//        params.put("statuses", excludedStatuses);
//
//        String query = "select udc from UserDataCoupon udc where udc.expiresAt <= :today and udc.status not in :statuses";
//
//        return new JpaPagingItemReaderBuilder<UserDataCoupon>()
//                .name("expireDataCouponReader")
//                .entityManagerFactory(entityManagerFactory)
//                .queryString(query)
//                .parameterValues(params)
//                .pageSize(100)
//                .build();
//    }
//
//    @Bean
//    public ItemProcessor<UserDataCoupon, UserDataCoupon> expireDataCouponProcessor() {
//        Status expiredStatus = statusService.getStatus("COUPON", "EXPIRED");
//        return coupon -> {
//            coupon.changeStatus(expiredStatus);
//            return coupon;
//        };
//    }
//
//    @Bean
//    public ItemWriter<UserDataCoupon> expireDataCouponWriter() {
//        return new JdbcBatchItemWriterBuilder<UserDataCoupon>()
//                .dataSource(dataSource)
//                .sql("update user_data_coupon set status_id = :statusId where user_data_coupon_id = :userDataCouponId")
//                .beanMapped()
//                .assertUpdates(false)
//                .build();
//    }

    /***************************************************/


    @Bean
    @StepScope
    public JdbcPagingItemReader<Long> expireDataCouponJdbcReader(
            @Value("#{jobParameters['currentTime']}") String currentTime) throws Exception {

        LocalDateTime today = LocalDateTime.parse(currentTime);
        Status expiredStatus = statusService.getStatus("COUPON", "EXPIRED");
        Status usedStatus = statusService.getStatus("COUPON", "USED");

        Map<String, Object> params = new HashMap<>();
        params.put("today", Timestamp.valueOf(today));
        params.put("expiredStatusId", expiredStatus.getStatusId());
        params.put("usedStatusId", usedStatus.getStatusId());

        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource); // 필드로 주입된 DataSource 필요
        factory.setSelectClause("SELECT udc.user_data_coupon_id");
        factory.setFromClause("FROM user_data_coupon udc");
        factory.setWhereClause("WHERE udc.expires_at <= :today AND udc.status_id NOT IN (:expiredStatusId, :usedStatusId)");

        Map<String, Order> sortKeys = new LinkedHashMap<>();
        sortKeys.put("udc.user_data_coupon_id", Order.ASCENDING);
        factory.setSortKeys(sortKeys);

        PagingQueryProvider provider = factory.getObject();

        return new JdbcPagingItemReaderBuilder<Long>()
                .name("expireDataCouponReader")
                .dataSource(dataSource)
                .queryProvider(provider)
                .parameterValues(params)
                .rowMapper((rs, rowNum) -> rs.getLong("user_data_coupon_id"))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemWriter<Long> expireDataCouponWriter() {
        Status expiredStatus = statusService.getStatus("COUPON", "EXPIRED");
        return new JdbcBatchItemWriterBuilder<Long>()
                .dataSource(dataSource)
                .sql("UPDATE user_data_coupon SET status_id = :statusId WHERE user_data_coupon_id = :userDataCouponId")
                .itemSqlParameterSourceProvider(item -> {
                    MapSqlParameterSource ps = new MapSqlParameterSource();
                    ps.addValue("userDataCouponId", item);
                    ps.addValue("statusId", expiredStatus.getStatusId());
                    return ps;
                })
                .assertUpdates(false)
                .build();
    }
//
//    @Bean
//    public ItemProcessor<Long, Long> failOnIdOneProcessor() {
//        return id -> {
//            if (id != null && id == 3L) {
//                throw new DataIntegrityViolationException("강제 실패: user_data_coupon_id=1"); // skip 대상이면 skip 처리됨
//            }
//            return id;
//        };
//    }

}
