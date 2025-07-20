package eureca.capstone.project.batch.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    @Builder
    public BatchFailureLog(String jobName, String stepName, String failedItemType, String failedItemId, String errorMessage) {
        this.jobName = jobName;
        this.stepName = stepName;
        this.failedItemType = failedItemType;
        this.failedItemId = failedItemId;
        this.errorMessage = errorMessage;
    }
}
