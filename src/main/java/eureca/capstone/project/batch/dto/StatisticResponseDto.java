package eureca.capstone.project.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long totalPrice;
    private Long transactionCount;
    private Long totalDataAmount;
}
