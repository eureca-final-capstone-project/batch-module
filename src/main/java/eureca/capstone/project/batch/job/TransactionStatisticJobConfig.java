package eureca.capstone.project.batch.job;

//import eureca.capstone.project.batch.component.listener.ExecutionListener;
import eureca.capstone.project.batch.component.listener.CustomRetryListener;
import eureca.capstone.project.batch.component.listener.CustomSkipListener;
import eureca.capstone.project.batch.component.listener.CustomWriterListener;
import eureca.capstone.project.batch.component.retry.RetryPolicy;
import eureca.capstone.project.batch.component.tasklet.BidStatisticSaveTasklet;
import eureca.capstone.project.batch.component.tasklet.NormalStatisticSaveTasklet;
import eureca.capstone.project.batch.component.writer.NormalStatisticWriter;
import eureca.capstone.project.batch.component.writer.BidVolumeStatisticWriter;
import eureca.capstone.project.batch.market_statistic.dto.TransactionHistoryStatisticDto;
import eureca.capstone.project.batch.transaction_feed.entity.DataTransactionHistory;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransactionStatisticJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final RetryPolicy retryPolicy;
//    private final ExecutionListener executionListener;
    private final NormalStatisticWriter normalStatisticWriter;
    private final BidVolumeStatisticWriter bidVolumeStatisticWriter;
    private final NormalStatisticSaveTasklet normalStatisticSaveTasklet;
    private final BidStatisticSaveTasklet bidStatisticSaveTasklet;
    private final DataSource dataSource;
    private final PagingQueryProvider pagingQueryProvider;
    private final CustomSkipListener customSkipListener;
    private final CustomWriterListener customWriterListener;

    // 일반판매 거래량 + 시세 통계 JOB
    @Bean
    public Job normalStatisticJob(
            Step normalStatisticCalculateStep,
            Step normalStatisticSaveStep
    ) {
        return new JobBuilder("normalStatisticJob", jobRepository)
                .start(normalStatisticCalculateStep) // 시세 통계, 거래량 조회 및 누적 집계
                .next(normalStatisticSaveStep)       // 통계 계산 및 저장
                .build();
    }

    // 입찰판매 거래량 통계 JOB
    @Bean
    public Job bidStatisticJob() {
        return new JobBuilder("bidStatisticJob", jobRepository)
                .start(bidStatisticCalculateStep()) // 거래량 조회 및 누적 집계
                .next(bidStatisticSaveStep())       // 통계 계산 및 저장
                .build();
    }

    //--------------------------- jdbc reader 사용하는 step ---------------------------//
    // 시세통계 + 일반판매 거래량 집계 STEP [normalStatisticJob]
    @Bean
    public Step normalStatisticCalculateStep(
            JdbcPagingItemReader<TransactionHistoryStatisticDto> transactionHistoryJdbcReader,
            CustomRetryListener customRetryListener) {
        return new StepBuilder("normalStatisticCalculateStep", jobRepository)
                .<TransactionHistoryStatisticDto, TransactionHistoryStatisticDto>chunk(100, platformTransactionManager)
                .reader(transactionHistoryJdbcReader)
                .writer(normalStatisticWriter)
                .faultTolerant()
                .skip(DataIntegrityViolationException.class)
                .skipLimit(3)
                .retryPolicy(retryPolicy.createRetryPolicy())
                .backOffPolicy(retryPolicy.createBackoffPolicy())
                .listener(customSkipListener)
                .listener(customRetryListener)
//                .listener(customWriterListener)
//                .listener(executionListener)
                .listener(promotionListener())
                .build();
    }


    //--------------------------- jpa reader 사용하는 step ---------------------------//
//@Bean
//public Step normalStatisticCalculateStep(
//        JpaPagingItemReader<TransactionHistoryStatisticDto> transactionHistoryJpaReader,
//        CustomRetryListener customRetryListener) {
//    return new StepBuilder("normalStatisticCalculateStep", jobRepository)
//            .<TransactionHistoryStatisticDto, TransactionHistoryStatisticDto>chunk(100, platformTransactionManager)
//            .reader(transactionHistoryJpaReader)
//            .writer(normalStatisticWriter)
//            .faultTolerant()
//            .skip(DataIntegrityViolationException.class)
//            .skipLimit(3)
//            .retryPolicy(retryPolicy.createRetryPolicy())
//            .backOffPolicy(retryPolicy.createBackoffPolicy())
//            .listener(customSkipListener)
//            .listener(customRetryListener)
////                .listener(customWriterListener)
////                .listener(executionListener)
//            .listener(promotionListener())
//            .build();
//}

    // 시세통계 + 일반판매 거래량 저장 STEP [normalStatisticJob]
    @Bean
    public Step normalStatisticSaveStep() {
        return new StepBuilder("normalStatisticSaveStep", jobRepository)
                .tasklet(normalStatisticSaveTasklet, platformTransactionManager)
                .build();
    }

    // 입찰판매 거래량 집계 STEP [bidStatisticJob]
    @Bean
    public Step bidStatisticCalculateStep() {
        return new StepBuilder("bidStatisticCalculateStep", jobRepository)
                .<DataTransactionHistory, DataTransactionHistory>chunk(100, platformTransactionManager)
                .reader(volumeJpaReader(null))
                .writer(bidVolumeStatisticWriter)
                .listener(promotionListener())
                .build();
    }

    // 입찰판매 거래량 저장 STEP [bidStatisticJob]
    @Bean
    public Step bidStatisticSaveStep() {
        return new StepBuilder("bidStatisticSaveStep", jobRepository)
                .tasklet(bidStatisticSaveTasklet, platformTransactionManager)
                .build();
    }



    // 시세통계 + 일반판매 거래량 READER [normalStatisticJob]
    @Bean
    @StepScope
    public JpaPagingItemReader<TransactionHistoryStatisticDto> transactionHistoryJpaReader(
            @Value("#{jobParameters['currentTime']}") String currentDate) {
        LocalDateTime currentTime = LocalDateTime.parse(currentDate);
        LocalDateTime from = currentTime
                .minusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime to = currentTime
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Map<String,Object> params = Map.of(
                "start", from,
                "end",   to,
                "typeId", 1
        );

        // 기존
//        String query = "select th from DataTransactionHistory th " +
//                "join fetch th.transactionFeed tf " +
//                "join fetch tf.telecomCompany tc " +
//                "join fetch tf.salesType st " +
//                "where th.createdAt >= :start and th.createdAt < :end and st.salesTypeId = :typeId";

        //dto
        String query = "select new eureca.capstone.project.batch.market_statistic.dto.TransactionHistoryStatisticDto(" +
                "tc.telecomCompanyId, tc.name, th.transactionFinalPrice, tf.salesDataAmount) " +
                "from DataTransactionHistory th " +
                "join th.transactionFeed tf " +
                "join tf.telecomCompany tc " +
                "join tf.salesType st " +
                "where th.createdAt >= :start and th.createdAt < :end and st.salesTypeId = :typeId";

        return new JpaPagingItemReaderBuilder<TransactionHistoryStatisticDto>()
                .name("transactionHistoryJpaReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }


    @Bean
    @StepScope
    public JdbcPagingItemReader<TransactionHistoryStatisticDto> transactionHistoryJdbcReader(
            PagingQueryProvider normalStatisticQueryProvider, // Qualifier 없이 주입
            @Value("#{jobParameters['currentTime']}") String currentDate) {

        LocalDateTime currentTime = LocalDateTime.parse(currentDate);
        LocalDateTime from = currentTime
                .minusHours(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime to = currentTime
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Map<String, Object> params = Map.of(
                "start", from,
                "end", to,
                "typeId", 1
        );

        // RowMapper는 DTO의 필드명과 SQL의 AS 별칭이 일치해야 합니다.
        // `th.created_at`은 통계 DTO에 필드가 없으므로 굳이 읽지 않아도 됩니다.
        RowMapper<TransactionHistoryStatisticDto> rowMapper = (rs, rowNum) ->
                TransactionHistoryStatisticDto.builder()
                        .telecomCompanyId(rs.getLong("telecomCompanyId"))
                        .telecomCompanyName(rs.getString("telecomCompanyName"))
                        .transactionFinalPrice(rs.getLong("transactionFinalPrice"))
                        .salesDataAmount(rs.getLong("salesDataAmount"))
                        .build();

        return new JdbcPagingItemReaderBuilder<TransactionHistoryStatisticDto>()
                .name("transactionHistoryJdbcReader")
                .dataSource(dataSource)
                .queryProvider(normalStatisticQueryProvider)
                .rowMapper(rowMapper)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }

    @Bean
    @StepScope
    public PagingQueryProvider normalStatisticQueryProvider(
            @Value("#{jobParameters['currentTime']}") String currentDate) throws Exception {

        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource);

        factory.setSelectClause(
                "SELECT tc.telecom_company_id AS telecomCompanyId, " +
                        "tc.name AS telecomCompanyName, " +
                        "th.transaction_final_price AS transactionFinalPrice, " +
                        "tf.sales_data_amount AS salesDataAmount, " +
                        "th.transaction_history_id"
        );

        factory.setFromClause(
                "FROM data_transaction_history th " +
                        "JOIN transaction_feed tf ON th.transaction_feed_id = tf.transaction_feed_id " +
                        "JOIN telecom_company tc ON tf.telecome_company_id = tc.telecom_company_id " + 
                        "JOIN sales_type st ON tf.sales_type_id = st.sales_type_id"
        );

        factory.setWhereClause(
                "WHERE st.sales_type_id = :typeId AND th.created_at >= :start AND th.created_at < :end"
        );


        Map<String, Order> sortKeys = new LinkedHashMap<>();
        sortKeys.put("th.transaction_history_id", Order.DESCENDING); // tiebreaker
        factory.setSortKeys(sortKeys);

        return factory.getObject();
    }



//    @Bean
//    @StepScope
//    public JdbcPagingItemReader<TransactionHistoryStatisticDto> transactionHistoryJdbcReader(
//            @Value("#{jobParameters['currentTime']}") String currentDate) {
//
//        LocalDateTime currentTime = LocalDateTime.parse(currentDate);
//        LocalDateTime from = currentTime
//                .minusHours(1)
//                .withMinute(0)
//                .withSecond(0)
//                .withNano(0);
//        LocalDateTime to = currentTime
//                .withMinute(0)
//                .withSecond(0)
//                .withNano(0);
//
//        Map<String,Object> params = Map.of(
//                "start", from,
//                "end",   to,
//                "typeId", 1
//        );
//
//        // JDBC Reader에 필요한 DTO 매핑용 RowMapper
//        RowMapper<TransactionHistoryStatisticDto> rowMapper = (rs, rowNum) -> TransactionHistoryStatisticDto.builder()
//                .telecomCompanyId(rs.getLong("telecomCompanyId"))
//                .telecomCompanyName(rs.getString("telecomCompanyName"))
//                .transactionFinalPrice(rs.getLong("transactionFinalPrice"))
//                .salesDataAmount(rs.getLong("salesDataAmount"))
//                .build();
//
//        // PagingQueryProvider를 사용하여 쿼리 생성
//        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
//        queryProvider.setDataSource(dataSource);
//        queryProvider.setSelectClause("select tc.telecom_company_id as telecomCompanyId, tc.name as telecomCompanyName, th.transaction_final_price as transactionFinalPrice, tf.sales_data_amount as salesDataAmount");
//        queryProvider.setFromClause("from data_transaction_history th " +
//                "join transaction_feed tf on th.transaction_feed_id = tf.transaction_feed_id " +
//                "join telecom_company tc on tf.telecome_company_id = tc.telecom_company_id " +
//                "join sales_type st on tf.sales_type_id = st.sales_type_id");
//        queryProvider.setWhereClause("where th.created_at >= :start and th.created_at < :end and st.sales_type_id = :typeId");
//        queryProvider.setSortKey(sortKeys); // created_at도 소문자로 변경
//
//        try {
//            PagingQueryProvider provider = queryProvider.getObject();
//            return new JdbcPagingItemReaderBuilder<TransactionHistoryStatisticDto>()
//                    .name("transactionHistoryJdbcReader")
//                    .dataSource(dataSource)
//                    .queryProvider(provider)
//                    .rowMapper(rowMapper)
//                    .parameterValues(params)
//                    .pageSize(100)
//                    .build();
//        } catch (Exception e) {
//            throw new RuntimeException("쿼리 프로바이더 생성 실패", e);
//        }
//    }


    // 입찰판매 거래량 READER [bidStatisticJob]
    @Bean
    @StepScope
    public JpaPagingItemReader<DataTransactionHistory> volumeJpaReader(
            @Value("#{jobParameters['currentTime']}") String currentTime) {

        LocalDate today = LocalDateTime.parse(currentTime).toLocalDate();
        LocalDateTime from  = today.atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        log.info("{} ~ {}", from, to);
        Map<String,Object> params = Map.of(
                "start", from,
                "end",   to,
                "typeId", 2
        );

        String query = "select th from DataTransactionHistory th " +
                "join fetch th.transactionFeed tf " +
                "join fetch tf.salesType st " +
                "where th.createdAt >= :start and th.createdAt < :end and st.salesTypeId = :typeId";

        return new JpaPagingItemReaderBuilder<DataTransactionHistory>()
                .name("volumeJpaReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }

    @Bean
    public ExecutionContextPromotionListener promotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {
                NormalStatisticWriter.NORMAL_STATISTIC_KEY,
                BidVolumeStatisticWriter.VOLUME_STATISTIC_KEY
        });
        return listener;
    }
}
