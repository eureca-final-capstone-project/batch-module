package eureca.capstone.project.batch.transaction_feed.repository;


import eureca.capstone.project.batch.transaction_feed.entity.Bids;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface BidsRepository extends JpaRepository<Bids, Long> {
    @Query("SELECT b FROM Bids b WHERE b.transactionFeed.transactionFeedId = :transactionFeedId ORDER BY b.bidAmount DESC, b.createdAt ASC")
    Optional<Bids> findTopByTransactionFeedIdOrderByBidAmountDescCreatedAtAsc(Long transactionFeedId);
}
