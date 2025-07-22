package eureca.capstone.project.batch.transaction_feed.repository;


import eureca.capstone.project.batch.transaction_feed.entity.Bids;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidsRepository extends JpaRepository<Bids, Long> {
    Optional<Bids> findTopByTransactionFeed_TransactionFeedIdOrderByBidAmountDescCreatedAtAsc(Long transactionFeedId);
}
