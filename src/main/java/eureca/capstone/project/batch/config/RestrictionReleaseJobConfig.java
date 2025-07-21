package eureca.capstone.project.batch.config;

import eureca.capstone.project.batch.auth.entity.UserAuthority;
import eureca.capstone.project.batch.auth.repository.UserAuthorityRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job restrictionReleaseJob() {
        return new JobBuilder("restrictionReleaseJob", jobRepository)
                .start(restrictionReleaseStep())
                .build();
    }

    @Bean
    public Step restrictionReleaseStep() {
        return new StepBuilder("restrictionReleaseStep", jobRepository)
                .<UserAuthority, UserAuthority>chunk(CHUNK_SIZE, transactionManager)
                .reader(restrictionReleaseReader())
                .processor(restrictionReleaseProcessor())
                .writer(restrictionReleaseWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<UserAuthority> restrictionReleaseReader() {
        JpaPagingItemReader<UserAuthority> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(CHUNK_SIZE);
        reader.setQueryString(
                "SELECT ua FROM UserAuthority ua WHERE ua.expiredAt IS NOT NULL AND ua.expiredAt <= :now"
        );
        reader.setParameterValues(Map.of("now", LocalDateTime.now()));
        reader.setName("restoreSanctionReader");
        return reader;
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
