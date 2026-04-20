package uk.gov.hmcts.reform.pcs.ccd.page.noticeofchange;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

/**
 * Page for confirming defendants date of birth.
 */
public class NoticeOfChangePage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noticeOfChange")
            .pageLabel("Notice of change")
            .label("noticeOfChange-line-separator", "---")
            .label(
                "noticeOfChange-text",
                """
                      <p class="govuk-body">You can use this notice of change (sometimes called a ‘notice of acting’) to acquire the case file from: </p>
                       <ul class="govuk-list govuk-list--bullet">
                            <li class="govuk-!-font-size-19">
                             a client acting in person</li>
                            <li class="govuk-!-font-size-19">
                             a solicitor previously acting on your client’s behalf</li>
                        </ul>

                      """)
            .mandatory(PCSCase::getEnterCaseNumber)
//            .complex(EnforcementOrder::getWarrantDetails)
//            .mandatory(WarrantDetails::getDefendantsDOBKnown)
//            .done()
//            .done()
            .label("noticeOfChange-save-and-return", SAVE_AND_RETURN);
    }
}
