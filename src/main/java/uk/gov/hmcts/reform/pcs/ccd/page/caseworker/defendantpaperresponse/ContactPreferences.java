package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse;

import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ContactPreferencesSelection;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.DefendantPaperResponseRequest;

public class ContactPreferences implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("contactPreferences")
            .pageLabel("2. Contact Preferences")
            .label("contactPreferences-lineSeparator", "---")
            .complex(PCSCase::getDefendantPaperResponse)
            .optional(DefendantPaperResponseRequest::getContactPreferences)
            .optional(
                DefendantPaperResponseRequest::getEmailAddress,
                ShowConditions.fieldContains(
                    "paperResponse_ContactPreferences",
                    ContactPreferencesSelection.BY_EMAIL
                )
            )
            .label("contactPreferences-lineSeparator-phone", "---")
            .label(
                "contactPreferences-phoneNumber",
                "2.2 If we need to contact the defendant with notifications or urgent updates about their case, "
                    + "what is their phone number? (Optional)"
            )
            .optional(DefendantPaperResponseRequest::getPhoneNumber)
            .label("contactPreferences-lineSeparator-bottom", "---")
            .done();
    }
}
