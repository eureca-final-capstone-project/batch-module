package eureca.capstone.project.batch.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_failure_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchFailureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String stepName;

    private String failedItemType;

    private String failedItemId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime failedAt;

    @Column(nullable = false)
    private boolean reprocessed = false;

    @LastModifiedDate
    private LocalDateTime reprocessedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FailureType failureType = FailureType.SKIP;

    @Builder
    public BatchFailureLog(String jobName, String stepName, String failedItemType,
                           String failedItemId, String errorMessage, FailureType failureType) {
        this.jobName = jobName;
        this.stepName = stepName;
        this.failedItemType = failedItemType;
        this.failedItemId = failedItemId;
        this.errorMessage = errorMessage;
        this.failureType = failureType != null ? failureType : FailureType.SKIP;
    }

    public void markAsReprocessed() {
        this.reprocessed = true;
        this.reprocessedAt = LocalDateTime.now();
    }

    public enum FailureType {
        SKIP("Skip"),
        RETRY_EXHAUSTED("Retry 소진"),
        JOB_FAILED("Job 실패");

        private final String description;

        FailureType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
