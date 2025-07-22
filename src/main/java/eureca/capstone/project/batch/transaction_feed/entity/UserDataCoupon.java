package eureca.capstone.project.batch.transaction_feed.entity;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "user_data_coupon")
public class UserDataCoupon extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_data_coupon_id")
    private Long userDataCouponId;

    @JoinColumn(name = "data_coupon_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private DataCoupon dataCoupon;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @JoinColumn(name = "status_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Status status;

    public void changeStatus(Status newStatus) {
        this.status = newStatus;
    }

    public Long getStatusId(){
        return status.getStatusId();
    }
}
