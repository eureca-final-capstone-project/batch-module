package eureca.capstone.project.batch.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataDto {
    private List<Long> userIds;
    private int amount;
}
