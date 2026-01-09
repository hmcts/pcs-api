package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Service
@AllArgsConstructor
public class CaseTitleService {

    private final AddressFormatter addressFormatter;

    /**
     * Builds a markdown string for the case title which appears near the top of every page in the case overview.
     * and events
     * @param pcsCase The current case data
     * @return A markdown string for the case title
     */
    public String buildCaseTitle(PCSCase pcsCase) {
        return """
                <p class="govuk-!-font-size-24 govuk-!-margin-top-0 govuk-!-margin-bottom-1">
                    Case number: ${[CASE_REFERENCE]}
                </p>
                <p class="govuk-!-font-size-24 govuk-!-margin-bottom-0">
                    Property address: %s
                </p>""".formatted(addressFormatter.formatShortAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER));

    }

}
