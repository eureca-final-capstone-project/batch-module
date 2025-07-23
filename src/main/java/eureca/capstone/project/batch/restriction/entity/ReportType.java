package eureca.capstone.project.batch.restriction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_type_id")
    private Long reportTypeId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String explanation;

    @Builder
    public ReportType(Long reportTypeId, String type, String explanation) {
        this.reportTypeId = reportTypeId;
        this.type = type;
        this.explanation = explanation;
    }

}
