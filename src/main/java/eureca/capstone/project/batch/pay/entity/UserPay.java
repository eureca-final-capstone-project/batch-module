package eureca.capstone.project.batch.pay.entity;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "user_pay")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserPay extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    private Long pay;

    public UserPay(User user) {
        this.user = user;
        this.pay = 0L;
    }

    public void charge(Long amount) {
        this.pay += amount;
    }

}
