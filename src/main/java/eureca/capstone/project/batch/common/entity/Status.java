package eureca.capstone.project.batch.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Objects;

@Builder
@Entity
@Table(name = "status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Status extends BaseEntity {

    @Id // ID 직접 할당하도록 변경
    @Column(name = "status_id")
    private Long statusId;

    @Column(nullable = false, length = 50)
    private String domain;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Status status = (Status) o;
        return Objects.equals(statusId, status.statusId) &&
                Objects.equals(code, status.code) &&
                Objects.equals(description, status.description) &&
                Objects.equals(domain, status.domain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusId, code, description, domain);
    }
}
