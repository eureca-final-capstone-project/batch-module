package eureca.capstone.project.batch.transaction_feed.repository;


import eureca.capstone.project.batch.transaction_feed.domain.DataTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataTransactionHistoryRepository extends JpaRepository<DataTransactionHistory, Long>{
}
