package uk.gov.hmcts.reform.pcs.noc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;

@Entity
@Table(name = "party_representation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyRepresentationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long caseReference;

    private UUID partyId;

    @Enumerated(EnumType.STRING)
    private PartyRole partyRole;

    private String organisationId;

    private String organisationName;

    private String caseRole;

    @Enumerated(EnumType.STRING)
    private PartyRepresentationStatus status;

    @Enumerated(EnumType.STRING)
    private PartyRepresentationSource source;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
