package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;

@AllArgsConstructor
@Component
public class SelectDefendant implements CcdPageConfiguration {

    private final DefendantResponseRepository defendantResponseRepository;
    private static final String ERROR_MESSAGE = "This defendant has already submitted a response";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectDefendant", this::midEvent)
            .pageLabel("Select defendant")
            .label("selectDefendant-lineSeparator", "---")
            .mandatory(PCSCase::getDefendantRadioList)
            .label("selectDefendant-lineSeparator-bottom", "---")
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        long caseId = details.getId();

        boolean hasDefendantResponse = defendantResponseRepository
            .existsByClaimPcsCaseCaseReferenceAndPartyId(caseId, caseData.getDefendantRadioList().getValueCode());
        String error = hasDefendantResponse ? ERROR_MESSAGE : null;

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errorMessageOverride(error).build();
    }
}
