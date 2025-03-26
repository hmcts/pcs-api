package uk.gov.hmcts.reform.pcs.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.entity.PartyRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CustomPcsCaseRepositoryImpl implements CustomPcsCaseRepository {

    // TODO: Consistent capitalisation for PCS and CCD

    private final EntityManager entityManager;

    public CustomPcsCaseRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<PCSCase> findDtoByCaseReference(long caseReference) {
        return entityManager.createQuery(
                """
                    select
                        pc.caseReference as caseReference,
                        a.addressLine1 as addressLine1,
                        a.addressLine2 as addressLine2,
                        a.addressLine3 as addressLine3,
                        a.postTown as addressTown,
                        a.county as addressCounty,
                        a.postCode as addressPostcode,
                        a.country as addressCountry,
                        c.summary as claimSummary,
                        p.id as partyId,
                        p.forename as partyForename,
                        p.surname as partySurname,
                        cp.role as partyRole
                    from PcsCase pc
                    left join Address a on pc.id = a.pcsCase.id
                    left join Claim c on pc.id = c.pcsCase.id
                    left join ClaimParty cp on c.id = cp.claim.id
                    left join Party p on p.id = cp.party.id
                    where pc.caseReference = :caseReference
                    """, Tuple.class
            )
            .setParameter("caseReference", caseReference)
            .getResultStream()
            .reduce(new ArrayList<>(), this::accumulatePCSCaseRow, this::combineLists)
            .stream().findFirst();
    }

    private List<PCSCase> accumulatePCSCaseRow(List<PCSCase> pcsCaseList, Tuple row) {
        Long rowCcdReference = row.get("caseReference", Long.class);
        PCSCase matchingCase = pcsCaseList.stream()
            .filter(pcsCase -> pcsCase.getCcdCaseReference().equals(rowCcdReference))
            .findFirst()
            .orElseGet(() -> {
                AddressUK propertyAddress = AddressUK.builder()
                    .addressLine1(row.get("addressLine1", String.class))
                    .addressLine2(row.get("addressLine2", String.class))
                    .addressLine3(row.get("addressLine3", String.class))
                    .postTown(row.get("addressTown", String.class))
                    .county(row.get("addressCounty", String.class))
                    .postCode(row.get("addressPostcode", String.class))
                    .country(row.get("addressCountry", String.class))
                    .build();

                PCSCase pcsCase = PCSCase.builder()
                    .ccdCaseReference(rowCcdReference)
                    .applicantForename("<placeholder>")
                    .propertyAddress(propertyAddress)
                    .claimants(new ArrayList<>())
                    .defendants(new ArrayList<>())
                    .interestedParties(new ArrayList<>())
                    .build();
                pcsCaseList.add(pcsCase);
                return pcsCase;
            });

        PartyRole partyRole = row.get("partyRole", PartyRole.class);
        if (partyRole != null) {
            // The left join would have a row with a null party_role ID if there were no claims or parties for a claim
            UUID partyId = row.get("partyId", UUID.class);
            Party party = Party.builder()
                .forename(row.get("partyForename", String.class))
                .surname(row.get("partySurname", String.class))
                .build();

            ListValue<Party> partyListValue = ListValue.<Party>builder()
                .id(partyId.toString())
                .value(party)
                .build();

            switch (partyRole) {
                case CLAIMANT -> matchingCase.getClaimants().add(partyListValue);
                case DEFENDANT -> matchingCase.getDefendants().add(partyListValue);
                case INTERESTED_PARTY -> matchingCase.getInterestedParties().add(partyListValue);
            }

        }

        return pcsCaseList;
    }

    private List<PCSCase> combineLists(List<PCSCase> list1, List<PCSCase> list2) {
        List<PCSCase> combined = new ArrayList<>(list1);
        combined.addAll(list2);
        return combined;
    }

}
