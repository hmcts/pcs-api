package uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd3.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static uk.gov.hmcts.reform.pcs.ccd3.domain.VerticalYesNo.YES;
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
                        If you’re not sure whether your claim is against trespassers,
                        <a href="https://www.gov.uk/squatting-law/remove-squatters"
                            rel="noreferrer noopener"
                            target="_blank"
                            class="govuk-link">read the guidance on removing squatters (opens in a new tab)</a>.
                        """)
            .mandatory(PCSCase::getClaimAgainstTrespassers);
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
