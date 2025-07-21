package eureca.capstone.project.batch.common.repository;

import eureca.capstone.project.batch.common.entity.TelecomCompany;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelecomCompanyRepository extends JpaRepository<TelecomCompany, Long> {
}
