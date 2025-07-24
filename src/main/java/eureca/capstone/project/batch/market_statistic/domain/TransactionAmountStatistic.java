package eureca.capstone.project.batch.market_statistic.domain;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.transaction_feed.entity.SalesType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "transaction_amount_statistics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"statics_time", "sales_type_id"})})
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionAmountStatistic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statisticsId;

    @Column(name = "transaction_amount")
    private long transactionAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_type_id")
    private SalesType salesType;

    @Column(name = "statistic_type") // HOUR/DAY
    private String statisticType;

    @Column(name = "statics_time")
    private LocalDateTime staticsTime;
}
