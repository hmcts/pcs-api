package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Service
@AllArgsConstructor
public class CaseTitleService {

    private final AddressFormatter addressFormatter;
    private final DocumentIdExtractor documentIdExtractor;

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
                </p>%s""".formatted(addressFormatter.formatShortAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER),
                                      caseBundleLink(pcsCase));
    }

    private String caseBundleLink(PCSCase pcsCase) {
        // BundleTabView supplies this document only to judges targeted by case-bundle-view-enabled.
        var tab = pcsCase.getBundleTab();
        if (tab == null || tab.getBundle() == null || tab.getBundle().getUrl() == null) {
            return "";
        }
        return """

                <p class="govuk-body govuk-!-margin-top-2 govuk-!-margin-bottom-0">
                    <a class="govuk-link" href="/documentsv2/%s/binary" target="_blank" rel="noopener noreferrer">Whole \
                file download (opens in new tab)</a>
                </p>""".formatted(documentIdExtractor.extractDocumentId(tab.getBundle().getUrl()));
    }

}
