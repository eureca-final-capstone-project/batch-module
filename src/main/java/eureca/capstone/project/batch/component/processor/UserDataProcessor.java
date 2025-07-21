package eureca.capstone.project.batch.component.processor;

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

        // 예외 발생시키기
//        if (userData.getUserDataId() == 2L || userData.getUserDataId() == 100L || userData.getUserDataId() == 1200L || userData.getUserDataId() == 200L) {  // 특정 ID일 때 일부러 오류 발생
//            throw new RuntimeException("의도적으로 발생시킨 Processor 예외");
//        }

        if(userData.getPlan() == null){
            throw new RuntimeException("Plan not found");
        }
        Long planData = userData.getPlan().getMonthlyDataMb();

        userData.resetTotalData(planData);
        userData.resetSellableData(0L);
        
        return userData;
    }
}
