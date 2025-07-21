package eureca.capstone.project.batch.common.repository;

import eureca.capstone.project.batch.common.entity.BatchFailureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface BatchFailureLogRepository extends JpaRepository<BatchFailureLog, Long> {

    @Query("SELECT f FROM BatchFailureLog f WHERE " +
            "(:jobName IS NULL OR f.jobName = :jobName) AND " +
            "(:stepName IS NULL OR f.stepName = :stepName) AND " +
            "(:failureTypes IS NULL OR f.failureType IN :failureTypes) AND " + // [수정] IN 절 사용
            "(:reprocessed IS NULL OR f.reprocessed = :reprocessed) " +         // [개선] reprocessed도 선택적 필터링
            "ORDER BY f.failedAt DESC")
    List<BatchFailureLog> findFailures(@Param("jobName") String jobName,
                                       @Param("stepName") String stepName,
                                       @Param("failureTypes") Set<BatchFailureLog.FailureType> failureTypes, // [수정] Set<Enum> 타입으로 변경
                                       @Param("reprocessed") Boolean reprocessed); // [개선] Boolean 래퍼 타입으로 변경
}
