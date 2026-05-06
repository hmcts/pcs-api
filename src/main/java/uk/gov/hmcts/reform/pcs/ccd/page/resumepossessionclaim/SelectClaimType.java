package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.YES;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

public class SelectClaimType implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectClaimType", this::midEvent)
            .pageLabel("Claim type")
            .label("selectClaimType-info", """
                        ---
                        <h3 class="govuk-heading-s govuk-!-font-size-19">Trespass claim</h3>
                        <p class="govuk-body">
                        A trespass claim can be made against someone who:
                        <ul class="govuk-list govuk-list--bullet">
                            <li class="govuk-!-font-size-19">
                            entered or remained in the property without your consent</li>
                            <li class="govuk-!-font-size-19">
                            is not a tenant, sub-tenant or licensee (even if a tenancy or licence has ended)</li>
                        </ul>
                        </p>
                        <p class="govuk-body">
                        This includes someone who:
                        <ul class="govuk-list govuk-list--bullet">
                            <li class="govuk-!-font-size-19">
                            had the licence terminated</li>
                            <li class="govuk-!-font-size-19">
                            who has no right to occupy because they did not succeed to the tenancy after a death</li>
                        </ul>
                        </p>
                        """)
            .mandatory(PCSCase::getClaimAgainstTrespassers)
            .label("selectClaimType-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        VerticalYesNo claimAgainstTrespassers = caseData.getClaimAgainstTrespassers();

        LegislativeCountry legislativeCountry = caseData.getLegislativeCountry();

        caseData.setShowClaimTypeNotEligibleEngland(YesOrNo.from(
            isEligibleInEngland(legislativeCountry, claimAgainstTrespassers)));

        caseData.setShowClaimTypeNotEligibleWales(YesOrNo.from(
            isEligibleInWales(legislativeCountry, claimAgainstTrespassers)));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private boolean isEligibleInEngland(LegislativeCountry legislativeCountry, VerticalYesNo claimAgainstTrespassers) {
        return legislativeCountry == ENGLAND && claimAgainstTrespassers == YES;
    }

    private Boolean isEligibleInWales(LegislativeCountry legislativeCountry, VerticalYesNo claimAgainstTrespassers) {
        return legislativeCountry == WALES && claimAgainstTrespassers == YES;
    }

}
