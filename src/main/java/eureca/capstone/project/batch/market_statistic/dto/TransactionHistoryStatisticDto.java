package eureca.capstone.project.batch.market_statistic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryStatisticDto {
    private Long telecomCompanyId;
    private String telecomCompanyName;
    private Long transactionFinalPrice;
    private Long salesDataAmount;
}
