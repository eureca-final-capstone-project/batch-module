package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.repository.StatusRepository;
import eureca.capstone.project.batch.transaction_feed.domain.TransactionFeed;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class ExpireGeneralSaleFeedJobConfig {
    private final EntityManagerFactory entityManagerFactory;
    private final StatusRepository statusRepository;
    private static final int CHUNK_SIZE=100;

    private final Status expiredStatus;

    public ExpireGeneralSaleFeedJobConfig(EntityManagerFactory entityManagerFactory,
                                          StatusRepository statusRepository){
        this.entityManagerFactory = entityManagerFactory;
        this.statusRepository = statusRepository;
        this.expiredStatus = statusRepository.findByDomainAndCode("FEED", "EXPIRED").orElseThrow(() -> new IllegalArgumentException("기간만료 상태를 찾을 수 없습니다."));
    }

    @Bean
    public Job expireGeneralSaleFeedJob(JobRepository jobRepository, Step expireGeneralSaleFeedStep){
        return new JobBuilder("expireGeneralSaleFeedJob", jobRepository)
                .start(expireGeneralSaleFeedStep)
                .build();
    }

    @Bean
    public Step expireGeneralSaleFeedStep(JobRepository jobRepository, PlatformTransactionManager transactionManager){
        return new StepBuilder("expireGeneralSaleFeedStep", jobRepository)
                .<TransactionFeed, TransactionFeed>chunk(CHUNK_SIZE, transactionManager)
                .reader(expireGeneralSaleFeedReader(null))
                .processor(expireGeneralSaleFeedProcessor())
                .writer(expireGeneralSaleFeedWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<TransactionFeed> expireGeneralSaleFeedReader(
            @Value("#{jobParameters['targetDateTime']}") String targetDateTimeStr){

        LocalDateTime targetDateTime = LocalDateTime.parse(targetDateTimeStr);

        String jpqlQuery = """
                SELECT tf FROM TransactionFeed tf
                JOIN tf.status s
                JOIN tf.salesType st
                WHERE st.name ='일반 판매'
                AND s.code = 'ON_SALE'
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
        return feed -> {
            feed.changeStatus(this.expiredStatus);
            return feed;
        };
    }

    @Bean
    public JpaItemWriter<TransactionFeed> expireGeneralSaleFeedWriter() {
        // JpaItemWriter를 사용하면 Processor에서 반환된 엔티티의 변경사항을 자동으로 DB에 반영(UPDATE)해줍니다.
        return new JpaItemWriterBuilder<TransactionFeed>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
