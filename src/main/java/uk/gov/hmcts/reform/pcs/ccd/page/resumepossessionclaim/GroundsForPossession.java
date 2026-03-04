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
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;


import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;


/**
 * Full implementation will be done in another ticket - responses not captured at the moment.
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
            .showWhen(when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                .is(TenancyLicenceType.ASSURED_TENANCY)
                .and(when(PCSCase::getLegislativeCountry).is(LegislativeCountry.ENGLAND)))
            .label("groundsForPossession-lineSeparator", "---")
            .mandatory(PCSCase::getClaimDueToRentArrears)
            .label("groundsForPossession-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        //resetting options
        if (caseData.getClaimDueToRentArrears() == YesOrNo.YES) {
            caseData.getNoRentArrearsGroundsOptions().setMandatoryGrounds(Set.of());
            caseData.getNoRentArrearsGroundsOptions().setDiscretionaryGrounds(Set.of());
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
