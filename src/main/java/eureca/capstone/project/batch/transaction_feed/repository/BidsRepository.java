package eureca.capstone.project.batch.transaction_feed.repository;


import eureca.capstone.project.batch.transaction_feed.entity.Bids;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BidsRepository extends JpaRepository<Bids, Long> {
    @Query("SELECT b FROM Bids b JOIN FETCH b.user u WHERE b.transactionFeed.transactionFeedId = :transactionFeedId ORDER BY b.bidAmount DESC, b.createdAt ASC")
    List<Bids> findHighestBidWithUser(Long transactionFeedId, Pageable pageable);
}
