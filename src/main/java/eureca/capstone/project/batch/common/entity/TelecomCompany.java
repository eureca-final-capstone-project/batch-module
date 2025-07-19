package eureca.capstone.project.batch.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "telecom_company")
public class TelecomCompany extends BaseEntity{

    @Column(name = "telecom_company_id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long telecomCompanyId;

    private String name;
}
