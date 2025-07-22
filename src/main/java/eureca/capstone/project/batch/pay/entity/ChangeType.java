package eureca.capstone.project.batch.pay.entity;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Objects;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "change_type")
public class ChangeType extends BaseEntity {
    @Id
    @Column(name = "change_type_id")
    private Long changeTypeId;

    private String type;
    private String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeType that = (ChangeType) o;
        return Objects.equals(changeTypeId, that.changeTypeId) &&
                Objects.equals(content, that.content) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeTypeId, content, type);
    }
}
