package uk.gov.hmcts.reform.pcs.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.service.CcdMoneyFieldFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CustomPcsCaseRepositoryImpl implements CustomPcsCaseRepository {

    private final EntityManager entityManager;
    private final CcdMoneyFieldFormatter ccdMoneyFieldFormatter;

    public CustomPcsCaseRepositoryImpl(EntityManager entityManager,
                                       CcdMoneyFieldFormatter ccdMoneyFieldFormatter) {

        this.entityManager = entityManager;
        this.ccdMoneyFieldFormatter = ccdMoneyFieldFormatter;
    }

    @Override
    public Optional<PcsCase> findDtoByCaseReference(long caseReference) {
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
                        c.id as claimId,
                        c.summary as claimSummary,
                        c.amount as claimAmount,
                        p.id as partyId,
                        p.forename as partyForename,
                        p.surname as partySurname,
                        p.active as partyActive,
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
            .reduce(new ArrayList<>(), this::accumulatePcsCaseRow, this::combineLists)
            .stream().findFirst();
    }

    private List<PcsCase> accumulatePcsCaseRow(List<PcsCase> pcsCaseList, Tuple row) {
        Map<UUID, Party> caseParties = new HashMap<>();

        PcsCase currentCase = findOrCreateCase(pcsCaseList, row);

        UUID claimId = row.get("claimId", UUID.class);
        if (claimId != null) {
            // The left join would have a row with a null claimId if there were no claims for a case
            Claim currentClaim = findOrCreateClaim(currentCase, claimId, row);

            UUID partyId = row.get("partyId", UUID.class);
            if (partyId != null) {
                // The left join would have a row with a null partyId if there were parties for this claim

                Party currentParty = findOrCreateParty(caseParties, partyId, row);

                PartyRole partyRole = row.get("partyRole", PartyRole.class);
                switch (partyRole) {
                    case CLAIMANT -> currentClaim.addClaimant(currentParty);
                    case DEFENDANT -> currentClaim.addDefendant(currentParty);
                    case INTERESTED_PARTY -> currentClaim.addInterestedParty(currentParty);
                }
            }
        }

        return pcsCaseList;
    }

    private static PcsCase findOrCreateCase(List<PcsCase> pcsCaseList, Tuple row) {
        Long rowCcdReference = row.get("caseReference", Long.class);

        return pcsCaseList.stream()
            .filter(pcsCase -> pcsCase.getCcdCaseReference().equals(rowCcdReference))
            .findFirst()
            .orElseGet(() -> {
                AddressUK propertyAddress = createAddress(row);

                PcsCase pcsCase = PcsCase.builder()
                    .ccdCaseReference(rowCcdReference)
                    .applicantForename("<placeholder>")
                    .propertyAddress(propertyAddress)
                    .claims(new ArrayList<>())
                    .build();

                pcsCaseList.add(pcsCase);

                return pcsCase;
            });
    }

    private static AddressUK createAddress(Tuple row) {
        return AddressUK.builder()
            .addressLine1(row.get("addressLine1", String.class))
            .addressLine2(row.get("addressLine2", String.class))
            .addressLine3(row.get("addressLine3", String.class))
            .postTown(row.get("addressTown", String.class))
            .county(row.get("addressCounty", String.class))
            .postCode(row.get("addressPostcode", String.class))
            .country(row.get("addressCountry", String.class))
            .build();
    }

    private Claim findOrCreateClaim(PcsCase pcsCase, UUID claimId, Tuple row) {
        return pcsCase.getClaims().stream()
            .filter(claim -> claim.getId().equals(claimId))
            .findFirst()
            .orElseGet(() -> {
                BigDecimal claimAmount = row.get("claimAmount", BigDecimal.class);

                Claim createdClaim = Claim.builder()
                    .id(claimId)
                    .summary(row.get("claimSummary", String.class))
                    .amountInPence(ccdMoneyFieldFormatter.formatToPenceString(claimAmount))
                    .claimants(new ArrayList<>())
                    .defendants(new ArrayList<>())
                    .interestedParties(new ArrayList<>())
                    .build();
                pcsCase.addClaim(createdClaim);
                return createdClaim;
            });

    }

    private Party findOrCreateParty(Map<UUID, Party> caseParties, UUID partyId, Tuple row) {
        Party party = caseParties.get(partyId);

        if (party == null) {
            party = Party.builder()
                .id(partyId)
                .forename(row.get("partyForename", String.class))
                .surname(row.get("partySurname", String.class))
                .active(YesOrNo.from(row.get("partyActive", Boolean.class)))
                .build();

            caseParties.put(partyId, party);
        }

        return party;
    }

    private List<PcsCase> combineLists(List<PcsCase> list1, List<PcsCase> list2) {
        List<PcsCase> combined = new ArrayList<>(list1);
        combined.addAll(list2);
        return combined;
    }

}
