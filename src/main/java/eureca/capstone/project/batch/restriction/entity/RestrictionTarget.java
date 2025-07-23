package eureca.capstone.project.batch.restriction.entity;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "restriction_target")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestrictionTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restriction_target_id")
    private Long restrictionTargetId;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_type_id")
    private ReportType reportType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restriction_type_id")
    private RestrictionType restrictionType;

    @JoinColumn(name = "status_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Status status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder
    public RestrictionTarget(User user, ReportType reportType, RestrictionType restrictionType, Status status, LocalDateTime expiresAt) {
        this.user = user;
        this.reportType = reportType;
        this.restrictionType = restrictionType;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void updateExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
