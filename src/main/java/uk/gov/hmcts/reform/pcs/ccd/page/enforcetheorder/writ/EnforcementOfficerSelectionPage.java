package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class EnforcementOfficerSelectionPage implements CcdPageConfiguration {

    public static final String APPLICATION_INFORMATION = """
                                <p class="govuk-body govuk-!-margin-bottom-1">If your application for a writ is
                                successful:
                                </p>
                                <ul class="govuk-list govuk-list--bullet">
                                  <li class="govuk-!-font-size-19">we’ll ask you to send a sealed copy of the writ to
                                  the
                                  National Information Centre for Enforcement (NICE)
                                  </li>
                                  <li class="govuk-!-font-size-19">NICE will choose a High Court enforcement officer
                                  (HCEO)
                                  for you
                                  </li>
                                </ul>
                                <p class="govuk-body govuk-!-margin-bottom-1">NICE will then contact you to tell you:
                                </p>
                                <ul class="govuk-list govuk-list--bullet">
                                  <li class="govuk-!-font-size-19">the name of your High Court enforcement officer
                                  </li>
                                  <li class="govuk-!-font-size-19">how to contact them, if you have any questions about
                                  the
                                  eviction
                                  </li>
                                </ul>
                                """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("EnforcementOfficerSelection")
            .pageLabel("The National Information Centre for Enforcement will choose a High Court enforcement officer "
                           + "for you")
            .showCondition("selectEnforcementType=\"WRIT\"" +
                               " AND writHasHiredHighCourtEnforcementOfficer=\"NO\"")
            .label("enforcementOfficerSelection-line-separator", "---")
            .label("enforcementOfficerSelection-notice", APPLICATION_INFORMATION);
    }
}
