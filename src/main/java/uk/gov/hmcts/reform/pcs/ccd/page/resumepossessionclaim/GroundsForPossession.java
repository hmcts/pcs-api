package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;


import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.common.MultiPageLabel.SAVE_AND_RETURN_HTML;


/**
 * Page configuration for the Grounds for Possession section.
 */
@AllArgsConstructor
@Component
@Slf4j
public class GroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundsForPossession", this::midEvent)
            .pageLabel("Grounds for possession")
            .showCondition("typeOfTenancyLicence!=\"SECURE_TENANCY\" "
                               + "AND typeOfTenancyLicence!=\"FLEXIBLE_TENANCY\"")
                .label("groundsForPossession-lineSeparator", "---")
                .mandatory(PCSCase::getGroundsForPossession)
                .label("groundsForPossession-saveAndResume", SAVE_AND_RETURN_HTML);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        //resetting options
        if (caseData.getGroundsForPossession() == YesOrNo.YES) {
            caseData.setNoRentArrearsMandatoryGroundsOptions(Set.of());
            caseData.setNoRentArrearsDiscretionaryGroundsOptions(Set.of());
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
