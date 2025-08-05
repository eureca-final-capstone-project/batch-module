package eureca.capstone.project.batch.common.dto.response; // ⚠️ 패키지 경로는 실제 프로젝트에 맞게 수정해주세요.

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class AIKeywordResponseDto {
    private List<String> keywords;
}