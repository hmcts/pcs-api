package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
public class LegalRepSelectDefendant implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectDefendant", this::midEvent)
            .pageLabel("Representation")
            .readonly(PCSCase::getMultipleRepresentedParties, NEVER_SHOW)
            .showCondition(fieldEquals("multipleRepresentedParties", VerticalYesNo.YES))
            .label("legalRepDocUpload-selectDef-lineSeparator", "---")
            .label("legalRepDocUpload-selectDef-question",
                   "### Which defendant are you uploading additional documents for?")
            .mandatoryWithLabel(PCSCase::getRepresentedPartyNames, "")
            .label("legalRepDocUpload-selectDef-lineSeparator-bottom", "---");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        DynamicListElement selectedPartyElement = caseData.getRepresentedPartyNames().getValue();
        caseData.setCurrentRepresentedPartyId(selectedPartyElement.getCode().toString());
        caseData.setCurrentRepresentedPartyName(selectedPartyElement.getLabel());

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
