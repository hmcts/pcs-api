package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

@AllArgsConstructor
@Component
public class MakeAClaim implements CcdPageConfiguration {

    private final EligibilityService eligibilityService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Make a claim", this::midEvent)
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
            PCSCase caseData = details.getData();
            String postcode = caseData.getPropertyAddress().getPostCode();
            EligibilityResult eligibilityResult = eligibilityService.checkEligibility(postcode, null);
            if (eligibilityResult.getStatus() == EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED) {
                caseData.setShowCrossBorderPage(YesOrNo.YES);
                caseData.setCrossBorderCountries(eligibilityResult.getLegislativeCountries()); // ### PICK UP FROM HERE
            } else {
                caseData.setShowCrossBorderPage(YesOrNo.NO);
            }
            
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
                                                                  }
}