package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.template.TemplateService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Component
@RequiredArgsConstructor
public class ConfirmationPanelBuilder {

    private final AddressFormatter addressFormatter;
    private final TemplateService templateService;

    public String buildStandardConfirmationPanel(String title, PCSCase caseData, long caseReference) {
        String formattedAddress = addressFormatter.formatShortAddress(caseData.getPropertyAddress(), COMMA_DELIMITER);

        List<String> bodyLines = List.of(
            "Case number #%s".formatted(caseReference),
            formattedAddress,
            caseData.getCaseNameHmctsInternal()
        );

        return buildConfirmationPanel(title, bodyLines);
    }

    public String buildConfirmationPanel(String title, List<String> bodyLines) {

        Map<String, Object> context = Map.of(
            "title", title,
            "bodyLines", bodyLines
        );

        return templateService.renderTemplate("confirmationPanel", context);
    }

}
