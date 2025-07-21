package eureca.capstone.project.batch.transaction_feed.domain;


import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table
@Getter
public class DataTransactionHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_history_id")
    private Long transactionHistoryId;

    @JoinColumn(name = "transaction_feed_id")
    @OneToOne(fetch = FetchType.LAZY)
    private TransactionFeed transactionFeed;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "transaction_final_price")
    private Long transactionFinalPrice;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
