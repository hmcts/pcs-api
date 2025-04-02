package uk.gov.hmcts.reform.pcs.postcode.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.audit.Audit;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "postcode_court_mapping")
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "postCode")
public class PostCode {

    @Id
    @Column(name = "postcode", length = 20, nullable = false)
    private String postCode;

    @Column(name = "epimid", nullable = false)
    private int epimId;

    @Column(name = "legislative_country", length = 80, nullable = false)
    private String legislativeCountry;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "audit", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Audit audit;

}
