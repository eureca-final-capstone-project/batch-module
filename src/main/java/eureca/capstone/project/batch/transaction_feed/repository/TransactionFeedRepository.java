package eureca.capstone.project.batch.transaction_feed.repository;

import eureca.capstone.project.batch.common.entity.Status;
import eureca.capstone.project.batch.transaction_feed.domain.TransactionFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionFeedRepository extends JpaRepository<TransactionFeed, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TransactionFeed tf SET tf.status = :expiredStatus WHERE tf.transactionFeedId IN :ids")
    int updateStatusForIds(@Param("expiredStatus") Status expiredStatus, @Param("ids") List<Long> ids);
}
