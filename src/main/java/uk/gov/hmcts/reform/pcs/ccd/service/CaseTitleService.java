package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

@Service
@AllArgsConstructor
public class CaseTitleService {

    private final AddressFormatter addressFormatter;

    /**
     * Builds a markdown string for the case title which appears near the top of every page in the case overview.
     * and events
     *
     * @param pcsCase            The current case data
     * @param currentUserDetails The current user details
     * @return A markdown string for the case title
     */
    public String buildCaseTitle(PCSCase pcsCase, UserInfo currentUserDetails) {

        return """
                <p class="govuk-!-font-size-24 govuk-!-margin-top-0 govuk-!-margin-bottom-1">
                    Case number: ${[CASE_REFERENCE]}
                </p>
                <p class="govuk-!-font-size-24 govuk-!-margin-bottom-1">
                    Property address: %s
                </p>
                <p class="govuk-!-font-size-24 govuk-!-margin-bottom-0">
                    Signed in as: %s
                </p>
                """.formatted(addressFormatter.formatAddressWithCommas(pcsCase.getPropertyAddress()),
                              currentUserDetails.getSub());
    }

}
