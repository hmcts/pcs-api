package uk.gov.hmcts.reform.pcs.factory;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PartyService;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@AllArgsConstructor
public class ClaimantPartyFactory {

    private final PartyService partyService;

    public PartyEntity createAndPersistClaimantParty(PCSCase pcsCase, ClaimantPartyContext context) {
        ClaimantInformation claimantInfo = getClaimantInfo(pcsCase);

        String claimantName = isNotBlank(claimantInfo.getOverriddenClaimantName())
            ? claimantInfo.getOverriddenClaimantName()
            : claimantInfo.getClaimantName();

        ClaimantContactPreferences contactPreferences = getContactPreferences(pcsCase);

        AddressUK contactAddress = contactPreferences.getOverriddenClaimantContactAddress() != null
            ? contactPreferences.getOverriddenClaimantContactAddress()
            : pcsCase.getPropertyAddress();

        String contactEmail = isNotBlank(contactPreferences.getOverriddenClaimantContactEmail())
            ? contactPreferences.getOverriddenClaimantContactEmail()
            : firstNonBlank(contactPreferences.getClaimantContactEmail(), context.userEmail());

        String organisationName = isNotBlank(claimantInfo.getOverriddenClaimantName())
            ? claimantInfo.getOverriddenClaimantName()
            : claimantInfo.getOrganisationName();

        if (isBlank(organisationName)) {
            organisationName = claimantName;
        }

        return partyService.createPartyEntity(
            context.userId(),
            claimantName,
            null,
            organisationName,
            contactEmail,
            contactAddress,
            contactPreferences.getClaimantContactPhoneNumber()
        );
    }

    private ClaimantInformation getClaimantInfo(PCSCase caseData) {
        return Optional.ofNullable(caseData.getClaimantInformation())
            .orElse(ClaimantInformation.builder().build());
    }

    private ClaimantContactPreferences getContactPreferences(PCSCase caseData) {
        return Optional.ofNullable(caseData.getClaimantContactPreferences())
            .orElse(ClaimantContactPreferences.builder().build());
    }

    private static String firstNonBlank(String a, String b) {
        return isNotBlank(a) ? a : b;
    }

    public record ClaimantPartyContext(java.util.UUID userId, String userEmail) { }

}
