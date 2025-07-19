package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.component.UserDataProcessor;
import eureca.capstone.project.batch.user.entity.UserData;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final UserDataProcessor userDataProcessor;

    @Bean
    public Job resetUserDataJob() {
        return new JobBuilder("resetUserDataJob", jobRepository)
                .start(resetUserDataStep())
                .build();
    }

    /*
        reader에서 오늘 초기화일자인 UserData 읽고,
        processor에서 UserData 초기화 처리
        writer에서 db에 저장
     */
    @Bean
    public Step resetUserDataStep() {
        return new StepBuilder("resetUserDataStep", jobRepository)
                .<UserData, UserData>chunk(100, platformTransactionManager)
                .reader(userDataJpaReader())
                .processor(userDataProcessor)
                .writer(userDataWriter())
                .faultTolerant()
                .retry(Exception.class)
                .retryLimit(3)
                .build();
    }

    /* TODO. 쿼리 성능 안좋음
        - plan 조회 쿼리가 user 수만큼 나감. (UserData - Plan 사이 연관관계 없음.)
        - userdata 재조회, update도 user 수만큼.
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<UserData> userDataJpaReader() {
        Integer today = LocalDate.now().getDayOfMonth();
        Map<String, Object> params = new HashMap<>();
        params.put("today", today);

        return new JpaPagingItemReaderBuilder<UserData>()
                .name("userJpaReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select ud from UserData ud where ud.resetDataAt = :today")
                .parameterValues(params)
                .pageSize(1000)
                .transacted(true)
                .build();
    }

    @Bean
    public JpaItemWriter<UserData> userDataWriter() {
        return new JpaItemWriterBuilder<UserData>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }


}
