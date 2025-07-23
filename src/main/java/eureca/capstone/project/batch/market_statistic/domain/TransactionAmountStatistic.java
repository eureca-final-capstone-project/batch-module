package eureca.capstone.project.batch.market_statistic.domain;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "transaction_amount_statistics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"statics_time"})})
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

    @Column(name = "statics_time")
    private LocalDateTime staticsTime;
}
