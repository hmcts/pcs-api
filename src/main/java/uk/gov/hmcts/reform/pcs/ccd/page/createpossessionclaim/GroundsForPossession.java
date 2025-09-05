package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;


import java.util.Set;

/**
 * Placeholder page configuration for the Grounds for Possession section.
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
                .label("groundsForPossession-lineSeparator", "---")
                .mandatory(PCSCase::getGroundsForPossession);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase pcsCase = details.getData();

        //resetting options
        pcsCase.setMandatoryGroundsOptionsList(Set.of());
        pcsCase.setDiscretionaryGroundsOptionsList(Set.of());

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(pcsCase)
            .build();
    }
}
