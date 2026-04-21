package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
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
@Table(name = "claim_party_legal_representative")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimPartyLegalRepresentativeEntity {

    @EmbeddedId
    @Builder.Default
    private LegalRepresentativePartyId id = new LegalRepresentativePartyId();

    @ManyToOne(fetch = LAZY)
    @MapsId("partyId")
    @JsonBackReference
    private PartyEntity party;

    @ManyToOne(fetch = LAZY)
    @MapsId("legalRepresentativeId")
    @JsonBackReference
    private LegalRepresentativeEntity legalRepresentative;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo active;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;
}
