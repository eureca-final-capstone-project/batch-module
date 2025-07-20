package eureca.capstone.project.batch.common.repository;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchFailureLogRepository extends JpaRepository<BatchFailureLog, Long> {
}
