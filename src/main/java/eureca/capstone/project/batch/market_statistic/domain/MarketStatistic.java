package eureca.capstone.project.batch.market_statistic.domain;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.common.entity.TelecomCompany;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "market_statistics")
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketStatistic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statisticsId;

    @Column(name = "average_price")
    private long averagePrice;

    @Column(name = "transaction_amount")
    private long transactionAmount;

    @Column(name = "statics_time")
    private LocalDateTime staticsTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telecom_company_id")
    private TelecomCompany telecomCompany;
}
