package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.COMMUNITY_LANDLORD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.PROVIDER_OF_SOCIAL_HOUSING;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@Component
@AllArgsConstructor
public class SelectClaimantType implements CcdPageConfiguration {

    private final DraftCaseDataService draftCaseDataService;
    private final SecurityContextService securityContextService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectClaimantType", this::midEvent)
            .pageLabel("Claimant type")
            .label("selectClaimantType-info", """
                        ---
                        A claimant is the person or organisation who is making the possession claim.
                        """)
            .readonly(PCSCase::getLegislativeCountry, NEVER_SHOW, true)
            .mandatory(PCSCase::getClaimantType)
            .label("selectClaimantType-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        long caseReference = details.getId();
        PCSCase caseData = details.getData();

        if (caseData.getResumeClaimKeepAnswers() == YesOrNo.NO) {
            draftCaseDataService.deleteUnsubmittedCaseData(caseReference, resumePossessionClaim);
            caseData.setHasUnsubmittedCaseData(YesOrNo.NO); // To hide the ResumeClaim page if navigating backwards
        }

        String selectedValue = caseData.getClaimantType().getValueCode();

        ClaimantType claimantType = ClaimantType.valueOf(selectedValue);

        LegislativeCountry legislativeCountry = caseData.getLegislativeCountry();

        caseData
            .setShowClaimantTypeNotEligibleEngland(YesOrNo.from(isEligibleInEngland(legislativeCountry, claimantType)));

        caseData
            .setShowClaimantTypeNotEligibleWales(YesOrNo.from(isEligibleInWales(legislativeCountry, claimantType)));


        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private boolean isEligibleInEngland(LegislativeCountry legislativeCountry, ClaimantType claimantType) {
        return legislativeCountry == ENGLAND && claimantType != PROVIDER_OF_SOCIAL_HOUSING;
    }

    private Boolean isEligibleInWales(LegislativeCountry legislativeCountry, ClaimantType claimantType) {
        return legislativeCountry == WALES && claimantType != COMMUNITY_LANDLORD;
    }

}
