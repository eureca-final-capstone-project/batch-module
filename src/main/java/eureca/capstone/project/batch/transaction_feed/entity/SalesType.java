package eureca.capstone.project.batch.transaction_feed.entity;

import eureca.capstone.project.batch.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "sales_type")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesType extends BaseEntity {
    @Column(name = "sales_type_id")
    @Id
    private Long salesTypeId;

    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SalesType salesType = (SalesType) o;
        return Objects.equals(salesTypeId, salesType.salesTypeId) && Objects.equals(name, salesType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(salesTypeId, name);
    }
}
