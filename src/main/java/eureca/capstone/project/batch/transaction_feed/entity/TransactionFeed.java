package eureca.capstone.project.batch.transaction_feed.entity;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.common.entity.TelecomCompany;
import eureca.capstone.project.batch.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "transaction_feed")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TransactionFeed extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "transaction_feed_id")
    private Long transactionFeedId;

    @JoinColumn(name = "seller_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name="title")
    private String title;

    @Column(name="content")
    private String content;

    @JoinColumn(name = "telecome_company_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private TelecomCompany telecomCompany;

    @JoinColumn(name = "sales_type_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private SalesType salesType;

    @Column(name = "sales_price")
    private Long salesPrice;

    @Column(name = "sales_data_amount")
    private Long salesDataAmount;

    @Column(name = "default_image_number")
    private Long defaultImageNumber;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @JoinColumn(name = "status_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Status status;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @Version
    private Long version;

    public void update(String title, String content, Long salesPrice, Long salesDataAmount, Long defaultImageNumber) {
        this.title = title;
        this.content = content;
        this.salesPrice = salesPrice;
        this.salesDataAmount = salesDataAmount;
        this.defaultImageNumber = defaultImageNumber;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void changeStatus(Status newStatus){
        this.status = newStatus;
    }
}
