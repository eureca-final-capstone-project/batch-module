package eureca.capstone.project.batch.job;

import eureca.capstone.project.batch.auth.entity.UserAuthority;
import eureca.capstone.project.batch.auth.repository.UserAuthorityRepository;
import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.util.StatusManager;
import eureca.capstone.project.batch.component.listener.CustomRetryListener;
import eureca.capstone.project.batch.component.listener.CustomSkipListener;
//import eureca.capstone.project.batch.component.listener.JobCompletionNotificationListener;
import eureca.capstone.project.batch.restriction.entity.RestrictionTarget;
import eureca.capstone.project.batch.restriction.repository.RestrictionTargetRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestrictionReleaseJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final UserAuthorityRepository userAuthorityRepository;
    private final RestrictionTargetRepository restrictionTargetRepository;
    private final StatusManager statusManager;
//    private final JobCompletionNotificationListener jobCompletionNotificationListener;
    private final CustomSkipListener customSkipListener;
    private final CustomRetryListener customRetryListener;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job restrictionReleaseJob() {
        return new JobBuilder("restrictionReleaseJob", jobRepository)
//                .listener(jobCompletionNotificationListener)
                .start(restrictionTargetExpirationStep())
                .next(restrictionReleaseStep())
                .build();
    }

    @Bean
    public Step restrictionTargetExpirationStep() {
        return new StepBuilder("restrictionTargetExpirationStep", jobRepository)
                .<RestrictionTarget, RestrictionTarget>chunk(CHUNK_SIZE, transactionManager)
                .reader(restrictionTargetExpirationReader(null))
                .processor(restrictionTargetExpirationProcessor())
                .writer(restrictionTargetExpirationWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(DataIntegrityViolationException.class)
                .skipLimit(10)
                .listener(customSkipListener)
                .listener(customRetryListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<RestrictionTarget> restrictionTargetExpirationReader(
            @Value("#{jobParameters['now']}") LocalDateTime now) {
        String jpql = """
                SELECT rt FROM RestrictionTarget rt
                JOIN FETCH rt.user u
                JOIN FETCH rt.status s
                WHERE rt.expiresAt < :now AND s.code = 'COMPLETED'
                """;

        return new JpaPagingItemReaderBuilder<RestrictionTarget>()
                .name("restrictionTargetExpirationReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(jpql)
                .parameterValues(Map.of("now", now))
                .build();
    }

    @Bean
    public ItemProcessor<RestrictionTarget, RestrictionTarget> restrictionTargetExpirationProcessor() {
        return restrictionTarget -> {
            log.info("제재 만료 처리할 RestrictionTarget: ID = {}, User ID = {}, ExpiresAt = {}",
                    restrictionTarget.getRestrictionTargetId(),
                    restrictionTarget.getUser().getUserId(),
                    restrictionTarget.getExpiresAt());
            return restrictionTarget;
        };
    }

    @Bean
    public ItemWriter<RestrictionTarget> restrictionTargetExpirationWriter() {
        return chunk -> {
            Status expiredStatus = statusManager.getStatus("RESTRICTION", "RESTRICT_EXPIRATION");
            List<Long> restrictionTargetIds = chunk.getItems().stream()
                    .map(RestrictionTarget::getRestrictionTargetId)
                    .toList();
            restrictionTargetRepository.bulkUpdateStatusForIds(restrictionTargetIds, expiredStatus);
        };
    }

    @Bean
    public Step restrictionReleaseStep() {
        return new StepBuilder("restrictionReleaseStep", jobRepository)
                .<UserAuthority, UserAuthority>chunk(CHUNK_SIZE, transactionManager)
                .reader(restrictionReleaseReader(null))
                .processor(restrictionReleaseProcessor())
                .writer(restrictionReleaseWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(DataIntegrityViolationException.class)
                .skipLimit(10)
                .listener(customSkipListener)
                .listener(customRetryListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<UserAuthority> restrictionReleaseReader(
            @Value("#{jobParameters['now']}") LocalDateTime now) {
        String jpql = """
                SELECT ua FROM UserAuthority ua
                JOIN FETCH ua.user u
                JOIN FETCH ua.authority a
                WHERE ua.expiredAt < :now
                """;

        return new JpaPagingItemReaderBuilder<UserAuthority>()
                .name("restrictionReleaseReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(jpql)
                .parameterValues(Map.of("now", now))
                .build();
    }

    @Bean
    public ItemProcessor<UserAuthority, UserAuthority> restrictionReleaseProcessor() {
        return userAuthority -> {
            log.info("만료된 제재를 삭제 처리합니다: User ID = {}, Authority = {}, ExpiredAt = {}",
                    userAuthority.getUser().getUserId(),
                    userAuthority.getAuthority().getName(),
                    userAuthority.getExpiredAt());
            return userAuthority;
        };
    }

    @Bean
    public ItemWriter<UserAuthority> restrictionReleaseWriter() {
        return chunk -> {
            List<UserAuthority> itemsToRemove = new ArrayList<>(chunk.getItems());
            userAuthorityRepository.deleteAllInBatch(itemsToRemove);
        };
    }
}