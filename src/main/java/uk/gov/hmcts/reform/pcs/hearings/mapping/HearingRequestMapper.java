package uk.gov.hmcts.reform.pcs.hearings.mapping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.hearings.model.CaseCategory;
import uk.gov.hmcts.reform.pcs.hearings.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.EntityRoleCode;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingLocation;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.OrganisationDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.PanelRequirements;
import uk.gov.hmcts.reform.pcs.hearings.model.PartyDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@Slf4j
@Component
public class HearingRequestMapper {

    private static final String HEARING_PRIORITY_STANDARD = "STANDARD";
    private static final String HEARING_CHANNEL_IN_PERSON = "INTER";
    private static final String LOCATION_TYPE_COURT = "COURT";
    private static final String CASE_CATEGORY_TYPE = "caseType";
    private static final String PUBLIC_CASE_NAME = "Possession claim";
    private static final String PARTY_TYPE_INDIVIDUAL = "IND";
    private static final String PARTY_TYPE_ORGANISATION = "ORG";

    private static final String CASE_DEEP_LINK_TEMPLATE = "%s/cases/case-details/%s";
    // TODO: remove once PostCodeCourtService is wired into case creation flow
    private static final String TEMP_CASE_MANAGEMENT_LOCATION = "20262";

    @Value("${hmc.serviceId}")
    private String hmctsServiceCode;

    @Value("${hmc.hearingType}")
    private String hearingType;

    @Value("${hmc.defaultHearingDurationMinutes}")
    private int defaultDuration;

    @Value("${exui.url}")
    private String exuiUrl;

    @Value("${hmc.temp-case-ref:}")
    private String tempCaseRef;

    public HearingRequest buildHearingRequest(long caseReference, PCSCase pcsCase) {
        List<PartyDetails> parties = buildPartyDetails(pcsCase);

        HearingRequest request = new HearingRequest();
        request.setCaseDetails(buildCaseDetails(Long.parseLong(tempCaseRef), pcsCase));
        request.setHearingDetails(buildHearingDetails(pcsCase, parties.size()));
        request.setPartyDetails(parties);
        return request;
    }

    private CaseDetails buildCaseDetails(long caseReference, PCSCase pcsCase) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setHmctsServiceCode(hmctsServiceCode);
        caseDetails.setCaseRef(String.valueOf(tempCaseRef));
        caseDetails.setCaseDeepLink(CASE_DEEP_LINK_TEMPLATE.formatted(exuiUrl, caseReference));
        // TODO: replace with a meaningful case name once requirements are defined
        caseDetails.setHmctsInternalCaseName("Possession claim " + caseReference);
        caseDetails.setPublicCaseName(PUBLIC_CASE_NAME);
        caseDetails.setCaseAdditionalSecurityFlag(false);
        caseDetails.setCaseInterpreterRequiredFlag(false);
        caseDetails.setCaseRestrictedFlag(false);
        caseDetails.setCaseSlaStartDate(LocalDate.now());

        // TODO: replace with pcsCase.getCaseManagementLocation() once PostCodeCourtService is wired into case creation
        String locationCode = pcsCase.getCaseManagementLocation() != null
            ? String.valueOf(pcsCase.getCaseManagementLocation())
            : TEMP_CASE_MANAGEMENT_LOCATION;
        caseDetails.setCaseManagementLocationCode(locationCode);

        CaseCategory caseCategory = new CaseCategory();
        caseCategory.setCategoryType(CASE_CATEGORY_TYPE);
        caseCategory.setCategoryValue(hmctsServiceCode);
        caseDetails.setCaseCategories(List.of(caseCategory));

        return caseDetails;
    }

    private HearingDetails buildHearingDetails(PCSCase pcsCase, int partyCount) {
        HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setAutoListFlag(false);
        hearingDetails.setHearingType(hearingType);
        hearingDetails.setDuration(defaultDuration);
        hearingDetails.setHearingPriorityType(HEARING_PRIORITY_STANDARD);
        hearingDetails.setHearingChannels(List.of(HEARING_CHANNEL_IN_PERSON));
        hearingDetails.setHearingIsLinkedFlag(false);
        hearingDetails.setPrivateHearingRequiredFlag(false);
        hearingDetails.setNumberOfPhysicalAttendees(partyCount);
        hearingDetails.setHearingInWelshFlag(WALES.equals(pcsCase.getLegislativeCountry()));
        hearingDetails.setPanelRequirements(new PanelRequirements());

        HearingLocation location = new HearingLocation();
        location.setLocationType(LOCATION_TYPE_COURT);
        // TODO: replace with pcsCase.getCaseManagementLocation() once PostCodeCourtService is wired into case creation
        String locationId = pcsCase.getCaseManagementLocation() != null
            ? String.valueOf(pcsCase.getCaseManagementLocation())
            : TEMP_CASE_MANAGEMENT_LOCATION;
        location.setLocationId(locationId);
        hearingDetails.setHearingLocations(List.of(location));

        return hearingDetails;
    }

    private List<PartyDetails> buildPartyDetails(PCSCase pcsCase) {
        List<PartyDetails> parties = new ArrayList<>();
        mapParties(pcsCase.getAllClaimants(), EntityRoleCode.CLAIMANT, parties);
        mapParties(pcsCase.getAllDefendants(), EntityRoleCode.DEFENDANT, parties);
        mapParties(pcsCase.getAllUnderlesseeOrMortgagees(), EntityRoleCode.UNDERLESSEE_OR_MORTGAGEE, parties);
        return parties;
    }

    private void mapParties(List<ListValue<Party>> partyList, EntityRoleCode roleCode, List<PartyDetails> result) {
        if (partyList == null) {
            return;
        }
        for (ListValue<Party> listValue : partyList) {
            Party party = listValue.getValue();
            if (party == null) {
                continue;
            }
            result.add(mapParty(party, roleCode));
        }
    }

    private PartyDetails mapParty(Party party, EntityRoleCode roleCode) {
        PartyDetails details = new PartyDetails();
        details.setPartyID(UUID.randomUUID().toString());
        details.setPartyRole(roleCode.getHmcReference());

        boolean isOrganisation = StringUtils.hasText(party.getOrgName())
            && !StringUtils.hasText(party.getFirstName())
            && !StringUtils.hasText(party.getLastName());

        if (isOrganisation) {
            details.setPartyType(PARTY_TYPE_ORGANISATION);
            OrganisationDetails orgDetails = new OrganisationDetails();
            orgDetails.setName(party.getOrgName());
            orgDetails.setOrganisationType(PARTY_TYPE_ORGANISATION);
            details.setOrganisationDetails(orgDetails);
        } else {
            details.setPartyType(PARTY_TYPE_INDIVIDUAL);
            IndividualDetails individualDetails = new IndividualDetails();
            individualDetails.setFirstName(party.getFirstName());
            individualDetails.setLastName(party.getLastName());
            if (StringUtils.hasText(party.getEmailAddress())) {
                individualDetails.setHearingChannelEmail(List.of(party.getEmailAddress()));
            }
            if (StringUtils.hasText(party.getPhoneNumber())) {
                individualDetails.setHearingChannelPhone(List.of(party.getPhoneNumber()));
            }
            details.setIndividualDetails(individualDetails);
        }

        return details;
    }
}
