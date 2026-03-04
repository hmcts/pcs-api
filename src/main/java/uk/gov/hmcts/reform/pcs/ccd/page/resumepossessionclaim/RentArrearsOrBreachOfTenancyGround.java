package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.FLEXIBLE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;

public class RentArrearsOrBreachOfTenancyGround implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("rentArrearsOrBreachOfTenancyGround", this::midEvent)
            .pageLabel(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY.getLabel())
            .showWhen(when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                .is(SECURE_TENANCY)
                .or(when(PCSCase::getTenancyLicenceDetails, TenancyLicenceDetails::getTypeOfTenancyLicence)
                    .is(FLEXIBLE_TENANCY))
                .and(when(PCSCase::getSecureOrFlexiblePossessionGrounds,
                    SecureOrFlexiblePossessionGrounds::getSecureOrFlexibleDiscretionaryGrounds)
                    .contains(RENT_ARREARS_OR_BREACH_OF_TENANCY))
                .and(when(PCSCase::getLegislativeCountry).is(LegislativeCountry.ENGLAND)))
            .label("rentArrearsOrBreachOfTenancyGround-lineSeparator", "---")
            .mandatory(PCSCase::getRentArrearsOrBreachOfTenancy)
            .label("rentArrearsOrBreachOfTenancyGround-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        if (caseData.getRentArrearsOrBreachOfTenancy().contains(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY)) {
            caseData.setShowBreachOfTenancyTextarea(YesOrNo.YES);
        } else {
            caseData.setShowBreachOfTenancyTextarea(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

}
