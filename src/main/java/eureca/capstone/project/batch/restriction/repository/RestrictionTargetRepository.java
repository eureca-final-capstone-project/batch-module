package eureca.capstone.project.batch.restriction.repository;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.restriction.entity.RestrictionTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RestrictionTargetRepository extends JpaRepository<RestrictionTarget, Long> {

    @Modifying
    @Query("UPDATE RestrictionTarget rt SET rt.status = :status WHERE rt.restrictionTargetId IN :ids")
    int bulkUpdateStatusForIds(@Param("ids") List<Long> ids, @Param("status") Status status);
}
