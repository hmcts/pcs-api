package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "legal_representative_org")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class LegalRepresentativeOrganisationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    private String organisationName;

    private String organisationId;

    private String email;

    private String phone;

    private String contactReference;

    @OneToOne(cascade = ALL,orphanRemoval = true)
    private AddressEntity address;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesOrNo hasAmendedContactDetails;

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "legalRepresentativeOrganisation")
    @Builder.Default
    @JsonManagedReference
    private List<PartyLegalRepresentativeOrganisationEntity> partyLegalRepresentativeOrganisationList =
        new ArrayList<>();

    @OneToMany(fetch = LAZY, cascade = ALL, mappedBy = "legalRepresentativeOrganisation")
    @Builder.Default
    @JsonManagedReference
    private List<LegalRepresentativeEntity> legalRepresentativeList = new ArrayList<>();

    public void addParty(PartyEntity party) {
        // if we have an existing inactive plroe that is inactive, then reactivate as it was previously linked
        Optional<PartyLegalRepresentativeOrganisationEntity> existingEntity =
            this.partyLegalRepresentativeOrganisationList
                .stream()
                .filter(e -> e.getParty().getId().equals(party.getId()))
                .findFirst();
        if (existingEntity.isPresent() && YesOrNo.NO.equals(existingEntity.get().getActive())) {
            PartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisationEntity = existingEntity
                .get();
            partyLegalRepresentativeOrganisationEntity.setActive(YesOrNo.YES);
            partyLegalRepresentativeOrganisationEntity.setStartDate(Instant.now());
            return;
        } else if (existingEntity.isPresent() && YesOrNo.YES.equals(existingEntity.get().getActive())) {
            log.warn("Party [{}] is already linked to Legal Representative Organisation [{}] and is active.",
                     party.getId(), this.getId());
            return;
        }

        PartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisationEntity =
            PartyLegalRepresentativeOrganisationEntity.builder()
            .legalRepresentativeOrganisation(this)
            .party(party)
            .startDate(Instant.now())
            .active(YesOrNo.YES)
            .build();
        partyLegalRepresentativeOrganisationList.add(partyLegalRepresentativeOrganisationEntity);
        party.getPartyLegalRepresentativeOrganisationList().add(partyLegalRepresentativeOrganisationEntity);
    }

    public void addLegalRepresentative(LegalRepresentativeEntity legalRepresentative) {
        legalRepresentativeList.add(legalRepresentative);
        legalRepresentative.setLegalRepresentativeOrganisation(this);
    }

}
