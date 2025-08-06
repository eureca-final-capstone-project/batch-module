package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.service.StatusService;
import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.retry.RetryPolicy;
import eureca.capstone.project.batch.transaction_feed.entity.UserDataCoupon;
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
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
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
    public Job expireDataCouponJob() throws Exception {
        return new JobBuilder("expireDataCouponJob", jobRepository)
                .start(expireDataCouponStep())
                .build();
    }

    @Bean
    public Step expireDataCouponStep() throws Exception {
        return new StepBuilder("expireDataCouponStep", jobRepository)
                .<UserDataCoupon, UserDataCoupon>chunk(100, platformTransactionManager)
                .reader(expireDataCouponReader(null, dataSource))
                .processor(expireDataCouponProcessor())
                .writer(expireDataCouponJdbcWriter())
                .faultTolerant()
                .retryPolicy(retryPolicy.createRetryPolicy())
                .backOffPolicy(retryPolicy.createBackoffPolicy())
                .listener(executionListener)
                .build();
    }


    @Bean
    @StepScope
    public JdbcPagingItemReader<UserDataCoupon> expireDataCouponReader(
            @Value("#{jobParameters['currentTime']}") String currentTime,
            DataSource dataSource) throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("today", LocalDateTime.parse(currentTime));
        Status expiredStatus = statusService.getStatus("COUPON", "EXPIRED");
        Status usedStatus = statusService.getStatus("COUPON", "USED");
        params.put("statuses", List.of(expiredStatus.getStatusId(), usedStatus.getStatusId()));

        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("select user_data_coupon_id, expires_at, data_coupon_id, status_id, user_id");
        queryProvider.setFromClause("from user_data_coupon");
        queryProvider.setWhereClause("where expires_at <= :today and status_id not in (:statuses)");
        queryProvider.setSortKey("user_data_coupon_id");

        return new JdbcPagingItemReaderBuilder<UserDataCoupon>()
                .name("expireDataCouponReader")
                .dataSource(dataSource)
                .pageSize(100) // 청크 사이즈와 동일하게 튜닝
                .fetchSize(100)
                .queryProvider(queryProvider.getObject())
                .parameterValues(params)
                // 👇 BeanPropertyRowMapper 대신 Custom RowMapper를 사용하여 객체를 완전하게 생성
                .rowMapper(userDataCouponRowMapper())
                .build();
    }

    @Bean
    public RowMapper<UserDataCoupon> userDataCouponRowMapper() {
        return (rs, rowNum) -> {
            // status_id 컬럼 값으로 Status 객체를 직접 만들어주어 연관관계 문제를 해결
            Status status = new Status(rs.getLong("status_id"));

            return UserDataCoupon.builder()
                    .userDataCouponId(rs.getLong("user_data_coupon_id"))
                    .expiresAt(rs.getTimestamp("expires_at").toLocalDateTime())
                    .status(status) // Status 객체를 직접 설정
                    // User, DataCoupon 등 다른 연관 객체도 필요하다면 여기서 동일하게 처리
                    .build();
        };
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
    public JdbcBatchItemWriter<UserDataCoupon> expireDataCouponJdbcWriter() {
        String sql = "UPDATE user_data_coupon SET status_id = :statusId WHERE user_data_coupon_id = :userDataCouponId";

        return new JdbcBatchItemWriterBuilder<UserDataCoupon>()
                .dataSource(dataSource)
                .sql(sql)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(false) // 👉 이 옵션 추가
                .build();
    }
}
