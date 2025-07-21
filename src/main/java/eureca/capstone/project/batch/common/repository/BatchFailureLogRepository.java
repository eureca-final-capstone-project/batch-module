package eureca.capstone.project.batch.common.repository;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchFailureLogRepository extends JpaRepository<BatchFailureLog, Long> {

    List<BatchFailureLog> findByJobNameAndStepNameAndReprocessed(
            String jobName, String stepName, boolean reprocessed);

    List<BatchFailureLog> findByJobNameAndReprocessed(String jobName, boolean reprocessed);

    List<BatchFailureLog> findByReprocessed(boolean reprocessed);

    List<BatchFailureLog> findByFailureTypeAndReprocessed(
            BatchFailureLog.FailureType failureType, boolean reprocessed);

    @Query("SELECT f FROM BatchFailureLog f WHERE " +
            "(:jobName IS NULL OR f.jobName = :jobName) AND " +
            "(:stepName IS NULL OR f.stepName = :stepName) AND " +
            "(:failureType IS NULL OR f.failureType = :failureType) AND " +
            "f.reprocessed = :reprocessed " +
            "ORDER BY f.failedAt DESC")
    List<BatchFailureLog> findFailures(@Param("jobName") String jobName,
                                       @Param("stepName") String stepName,
                                       @Param("failureType") BatchFailureLog.FailureType failureType,
                                       @Param("reprocessed") boolean reprocessed);
}
