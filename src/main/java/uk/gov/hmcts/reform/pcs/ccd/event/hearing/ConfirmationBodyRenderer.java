package uk.gov.hmcts.reform.pcs.ccd.event.hearing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

@Component
@RequiredArgsConstructor
public class ConfirmationBodyRenderer {

    private final ConfirmationPanelBuilder confirmationPanelBuilder;

    public String renderHearingAddedConfirmationBody(PCSCase caseData, long caseReference) {
        String confirmationPanel = confirmationPanelBuilder
            .buildStandardConfirmationPanel("Hearing added", caseData, caseReference);

        return """
            ---
            %s

            <h3>What happens next</h3>

            A hearing notice will be issued if you specified one is needed.
            """.formatted(confirmationPanel);
    }

    public String renderHearingEditedConfirmationBody(PCSCase caseData, long caseReference) {
        String confirmationPanel = confirmationPanelBuilder
            .buildStandardConfirmationPanel("Hearing edited", caseData, caseReference);

        return """
            ---
            %s

            <h3>What happens next</h3>

            A hearing notice will be issued if you specified one is needed.
            """.formatted(confirmationPanel);
    }

    public String renderHearingCancelledConfirmationBody(PCSCase caseData, long caseReference) {
        String confirmationPanel = confirmationPanelBuilder
            .buildStandardConfirmationPanel("Hearing cancelled", caseData, caseReference);

        return """
            ---
            %s

            <h3>What happens next</h3>

            A cancellation notice will be sent to the parties.
            """.formatted(confirmationPanel);
    }

}
