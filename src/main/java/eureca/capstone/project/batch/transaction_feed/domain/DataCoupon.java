package eureca.capstone.project.batch.transaction_feed.domain;


import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.common.entity.TelecomCompany;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "data_coupon", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"data_amount", "telecom_company_id"})
})
public class DataCoupon extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_coupon_id")
    private Long dataCouponId;

    @Column(name = "coupon_number")
    private String couponNumber;

    @Column(name = "data_amount")
    private Long dataAmount;

    @JoinColumn(name = "telecom_company_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private TelecomCompany telecomCompany;
}
