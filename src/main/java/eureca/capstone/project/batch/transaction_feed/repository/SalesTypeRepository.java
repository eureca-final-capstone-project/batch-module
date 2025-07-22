package eureca.capstone.project.batch.transaction_feed.repository;


import eureca.capstone.project.batch.transaction_feed.entity.SalesType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesTypeRepository extends JpaRepository<SalesType, Long> {
}
