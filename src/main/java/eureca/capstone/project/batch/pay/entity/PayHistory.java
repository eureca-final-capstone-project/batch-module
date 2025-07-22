package eureca.capstone.project.batch.pay.entity;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "pay_history")
public class PayHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_history_id")
    private Long payHistoryId;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "change_type_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private ChangeType changeType;

    @Column(name = "changed_pay")
    private Long changedPay;

    @Column(name = "final_pay")
    private Long finalPay;
}
