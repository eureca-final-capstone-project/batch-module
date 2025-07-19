package eureca.capstone.project.batch.user.repository;

import eureca.capstone.project.batch.user.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    @Query("select p.monthlyDataMb from Plan p where p.planId=:planId")
    Optional<Long> findDataByPlanId(Long planId);
}
