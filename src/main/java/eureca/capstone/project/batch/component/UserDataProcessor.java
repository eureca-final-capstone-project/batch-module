package eureca.capstone.project.batch.component;

import eureca.capstone.project.batch.user.entity.UserData;
import eureca.capstone.project.batch.user.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDataProcessor implements ItemProcessor<UserData, UserData> {

    private final PlanRepository planRepository;

    @Override
    public UserData process(UserData userData) throws Exception {
        Long planData = planRepository.findDataByPlanId(userData.getPlanId())
                .orElseThrow(() -> new RuntimeException("PlanData not found"));

        userData.resetTotalData(planData);
        userData.resetSellableData(0L);

        return userData;
    }
}
