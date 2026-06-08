package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackAddress;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackAddressRow;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackDefendantRow;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackParty;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackUnderlesseeRow;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatDefendantHeading;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatUnderlesseeHeading;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isNo;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isPopulated;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.toClaimPackAddress;

/**
 * Maps the claimant, defendants and underlessees/mortgagees onto the claim-pack payload,
 * plus the derived case name.
 */
@Service
@AllArgsConstructor
class ClaimPackPartyMapper {

    private final CaseNameFormatter caseNameFormatter;

    void mapClaimant(List<PartyEntity> claimants, ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (claimants.isEmpty()) {
            return;
        }
        ClaimPackParty claimant = toClaimPackParty(claimants.getFirst());
        payloadBuilder.claimant(claimant);
        payloadBuilder.claimantDisplayName(derivePartyDisplayName(claimants.getFirst()));
        ClaimPackAddress address = claimant.getAddress();
        payloadBuilder.hasClaimantAddressLine2(address != null && isPopulated(address.getAddressLine2()));
        payloadBuilder.hasClaimantAddressLine3(address != null && isPopulated(address.getAddressLine3()));
        payloadBuilder.hasClaimantCounty(address != null && isPopulated(address.getCounty()));
    }

    void mapDefendants(List<PartyEntity> defendants, AddressEntity propertyAddress,
                       ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        List<ClaimPackDefendantRow> rows = new ArrayList<>(defendants.size());
        int number = 1;
        for (PartyEntity defendant : defendants) {
            rows.add(toDefendantRow(defendant, number++, propertyAddress));
        }
        payloadBuilder.defendants(rows);
    }

    void mapUnderlessees(List<PartyEntity> underlessees,
                         ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        List<ClaimPackUnderlesseeRow> rows = new ArrayList<>(underlessees.size());
        int number = 1;
        for (PartyEntity underlessee : underlessees) {
            rows.add(toUnderlesseeRow(underlessee, number++));
        }
        payloadBuilder.underlessees(rows);
    }

    void mapCaseName(List<PartyEntity> claimants, List<PartyEntity> defendants,
                     ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (claimants.isEmpty() && defendants.isEmpty()) {
            return;
        }
        List<Party> claimantParties = claimants.stream().map(ClaimPackPartyMapper::toDomainParty).toList();
        List<Party> defendantParties = defendants.stream().map(ClaimPackPartyMapper::toDomainParty).toList();
        payloadBuilder.caseName(caseNameFormatter.formatCaseName(claimantParties, defendantParties));
    }

    // A defendant with no address of their own falls back to the property address
    // (the claim form can't show a defendant address as "unknown").
    private ClaimPackDefendantRow toDefendantRow(PartyEntity defendant, int number, AddressEntity propertyAddress) {
        ClaimPackDefendantRow row = ClaimPackDefendantRow.builder()
            .defendantNumber(number)
            .heading(formatDefendantHeading(number))
            .displayName(derivePartyDisplayName(defendant))
            .build();
        applyAddress(row, pickAddressOrFallback(defendant.getAddress(), propertyAddress));
        return row;
    }

    // Each underlessee/mortgagee renders either a full address or a single "Address unknown" line.
    private ClaimPackUnderlesseeRow toUnderlesseeRow(PartyEntity underlessee, int number) {
        AddressEntity address = underlessee.getAddress();
        boolean addressKnown = address != null && isPopulated(address.getAddressLine1());
        ClaimPackUnderlesseeRow row = ClaimPackUnderlesseeRow.builder()
            .underlesseeNumber(number)
            .heading(formatUnderlesseeHeading(number))
            .displayName(derivePartyDisplayName(underlessee))
            .addressKnown(addressKnown)
            .addressUnknown(!addressKnown)
            .build();
        applyAddress(row, addressKnown ? address : null);
        return row;
    }

    // Populate the shared address fields. A null address (or unknown underlessee address) leaves the
    // rows unset so they don't render.
    private static void applyAddress(ClaimPackAddressRow row, AddressEntity address) {
        if (address == null) {
            return;
        }
        row.setAddressLine1(address.getAddressLine1());
        row.setAddressLine2(address.getAddressLine2());
        row.setAddressLine3(address.getAddressLine3());
        row.setPostTown(address.getPostTown());
        row.setCounty(address.getCounty());
        row.setPostcode(address.getPostcode());
        row.setHasAddressLine2(isPopulated(address.getAddressLine2()));
        row.setHasAddressLine3(isPopulated(address.getAddressLine3()));
        row.setHasCounty(isPopulated(address.getCounty()));
    }

    private static AddressEntity pickAddressOrFallback(AddressEntity defendantAddress, AddressEntity propertyAddress) {
        if (defendantAddress != null && isPopulated(defendantAddress.getAddressLine1())) {
            return defendantAddress;
        }
        return propertyAddress;
    }

    private static ClaimPackParty toClaimPackParty(PartyEntity party) {
        return ClaimPackParty.builder()
            .firstName(party.getFirstName())
            .lastName(party.getLastName())
            .orgName(party.getOrgName())
            .isPersonsUnknown(isNo(party.getNameKnown()))
            .address(toClaimPackAddress(party.getAddress()))
            .build();
    }

    private static Party toDomainParty(PartyEntity party) {
        return Party.builder()
            .firstName(party.getFirstName())
            .lastName(party.getLastName())
            .orgName(party.getOrgName())
            .nameKnown(party.getNameKnown())
            .build();
    }

    // Org name, else "Persons unknown" when the name isn't known or is absent, else "first last".
    private static String derivePartyDisplayName(PartyEntity party) {
        if (isPopulated(party.getOrgName())) {
            return party.getOrgName();
        }
        if (isNo(party.getNameKnown())) {
            return "Persons unknown";
        }
        String name = joinName(party.getFirstName(), party.getLastName());
        return name.isEmpty() ? "Persons unknown" : name;
    }

    private static String joinName(String firstName, String lastName) {
        boolean hasFirst = isPopulated(firstName);
        boolean hasLast = isPopulated(lastName);
        return (hasFirst ? firstName : "") + (hasFirst && hasLast ? " " : "") + (hasLast ? lastName : "");
    }
}
