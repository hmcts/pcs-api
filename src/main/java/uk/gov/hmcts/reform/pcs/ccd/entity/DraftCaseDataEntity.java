package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "draft", name = "draft_case_data")
@Getter
@Setter
public class DraftCaseDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Long caseReference;

    @JdbcTypeCode(SqlTypes.JSON)
    private String caseData;

    @Enumerated(EnumType.STRING)
    private EventId eventId;

    private UUID idamUserId;

    private UUID partyId;

    private String organisationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Optimistic lock: incremented on every save. Exposed to the UI as PossessionClaimResponse.draftVersion so a
    // submit can prove which draft the citizen reviewed (HDPI-8866 W05).
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
