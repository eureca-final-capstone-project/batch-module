package eureca.capstone.project.batch.transaction_feed.document;

import eureca.capstone.project.batch.transaction_feed.entity.TransactionFeed;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "transaction_feed")
@Setting(settingPath = "elasticsearch/analyzer-settings.json")
public class TransactionFeedDocument {

    @Id
    private Long id;

    @MultiField(
            mainField = @Field(
                    type = FieldType.Text,
                    analyzer = "index_analyzer",
                    searchAnalyzer = "search_analyzer"
            ),
            otherFields = {
                    @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "prefix_analyzer")
            }
    )
    private String title;

    @MultiField(
            mainField = @Field(
                    type = FieldType.Text,
                    analyzer = "index_analyzer",
                    searchAnalyzer = "search_analyzer"
            ),
            otherFields = {
                    @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "prefix_analyzer")
            }
    )
    private String content;

    @Field(type = FieldType.Long)
    private Long salesPrice;

    @Field(type = FieldType.Long)
    private Long currentHighestPrice;

    @Field(type = FieldType.Long)
    private Long sortPrice;

    @Field(type = FieldType.Long)
    private Long salesDataAmount;

    @Field(type = FieldType.Keyword)
    private Long sellerId;

    @MultiField(
            mainField = @Field(
                    type = FieldType.Text,
                    analyzer = "index_analyzer",
                    searchAnalyzer = "search_analyzer"
            ),
            otherFields = {
                    @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "prefix_analyzer")
            }
    )
    private String nickname;

    @Field(type = FieldType.Keyword)
    private Long telecomCompanyId;

    private String telecomCompanyName; // 검색용

    @Field(type = FieldType.Keyword)
    private Long salesTypeId;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private Long defaultImageNumber;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime expiresAt;

    @Field(type = FieldType.Boolean)
    private boolean isDeleted;

    public static TransactionFeedDocument fromEntity(TransactionFeed transactionFeed) {
        Long initialPrice = transactionFeed.getSalesPrice();

        return TransactionFeedDocument.builder()
                .id(transactionFeed.getTransactionFeedId())
                .title(transactionFeed.getTitle())
                .content(transactionFeed.getContent())
                .salesPrice(initialPrice)
                .currentHighestPrice(initialPrice)
                .sortPrice(initialPrice)
                .salesDataAmount(transactionFeed.getSalesDataAmount())
                .sellerId(transactionFeed.getUser().getUserId())
                .nickname(transactionFeed.getUser().getNickname())
                .telecomCompanyId(transactionFeed.getTelecomCompany().getTelecomCompanyId())
                .telecomCompanyName(transactionFeed.getTelecomCompany().getName())
                .salesTypeId(transactionFeed.getSalesType().getSalesTypeId())
                .status(transactionFeed.getStatus().getCode())
                .defaultImageNumber(transactionFeed.getDefaultImageNumber())
                .createdAt(transactionFeed.getCreatedAt())
                .expiresAt(transactionFeed.getExpiresAt())
                .isDeleted(transactionFeed.isDeleted())
                .build();
    }

    public void updateHighestPrice(Long price) {
        this.currentHighestPrice = price;
        this.sortPrice = price;
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}
