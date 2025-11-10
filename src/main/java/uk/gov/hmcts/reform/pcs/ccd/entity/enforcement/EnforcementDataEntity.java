package uk.gov.hmcts.reform.pcs.ccd.entity.enforcement;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "enforcement", name = "enf_case")
@Getter
@Setter
public class EnforcementDataEntity {

    @Id
    @Column(name = "enforcement_case_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "submitted_enforcement_data")
    private String enforcementData;
}
