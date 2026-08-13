package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

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

    /**
     * The organisation that owns this draft, when it is owned by one rather than by an individual.
     * When set it, not {@link #idamUserId}, identifies the draft; {@code idamUserId} then records only
     * who last wrote it. Matches the organisation the case's CaseAccessGroups are derived from, so a
     * draft is reachable by exactly the people the case is.
     */
    private String organisationId;

}
