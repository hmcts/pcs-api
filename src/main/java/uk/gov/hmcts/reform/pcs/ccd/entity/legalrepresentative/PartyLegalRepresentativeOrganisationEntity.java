package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "party_legal_rep_org")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyLegalRepresentativeOrganisationEntity {

    @EmbeddedId
    @Builder.Default
    private LegalRepresentativeOrganisationPartyId id = new LegalRepresentativeOrganisationPartyId();

    @ManyToOne(fetch = LAZY)
    @MapsId("partyId")
    @JsonBackReference
    private PartyEntity party;

    @ManyToOne(fetch = LAZY)
    @MapsId("legalRepresentativeOrganisationId")
    @JsonBackReference
    private LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo active;

    private Instant startDate;

    private Instant endDate;
}
