package eureca.capstone.project.batch.transaction_feed.repository;


import eureca.capstone.project.batch.transaction_feed.entity.SalesType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesTypeRepository extends JpaRepository<SalesType, Long> {
    Optional<SalesType> findByName(String name);
}
