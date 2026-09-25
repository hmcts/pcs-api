package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.ManagePartyOptions;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.RemovePartyDetails;

import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty.RemovePartyService.PARTY_CANNOT_BE_REMOVED_ERROR;

@Component
public class RemovePartyDetailsPage implements CcdPageConfiguration {

    private static final String CANNOT_REMOVE_PARTY_GUIDANCE = """
        <p class="govuk-body govuk-!-font-size-19">
        You must return to the previous screen and select a different party to continue
        </p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("removePartyDetails", this::midEvent)
            .showCondition(ShowConditions.fieldEquals("addParty_ManagePartyOptions", ManagePartyOptions.REMOVE_PARTY))
            .pageLabel("Check full party details")
            .label("removePartyDetails-separator", "---")
            .label("removePartyDetails-partyDetails", "## Party details")
            .complex(PCSCase::getRemovePartyDetails)
                .readonlyNoSummary(RemovePartyDetails::getSelectedPartyLabel)
                .readonlyNoSummary(RemovePartyDetails::getDateOfBirth)
                .readonlyNoSummary(RemovePartyDetails::getAddress)
                .mandatory(RemovePartyDetails::getRemoveSelectedParty)
                .label(
                    "removePartyDetails-cannotRemovePartyGuidance",
                    CANNOT_REMOVE_PARTY_GUIDANCE,
                    "removeParty_RemoveSelectedParty=\"No\"",
                    false)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details, CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        if (caseData.getRemovePartyDetails().getRemoveSelectedParty() == YesOrNo.NO) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errorMessageOverride(PARTY_CANNOT_BE_REMOVED_ERROR)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData).build();
    }
}
