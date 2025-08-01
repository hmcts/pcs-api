package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;


public class ContactPreferences implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("contactPreferences")
            .pageLabel("Contact Preferences")

            // Email section
            .readonly(PCSCase::getContactEmail, NEVER_SHOW)
            .label("contactPreferences-info-email", """
                ---
                <h2 class="govuk-heading-m">Notifications</h2>
                <p class="govuk-body-m govuk-!-margin-bottom-1">
                    You'll receive updates about your claim by email. For example, when a hearing
                    has been scheduled or when a document is ready to view.
                </p>
                  <h3 class="govuk-heading-m govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    Your My HMCTS registered email address is:
                </h3>
                <p class="govuk-body-s govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    ${contactEmail}
                </p>
                """)
            .label("contactPreferences-email-question", """
                <h3 class="govuk-heading-m govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    Do you want to use this email address for notifications?
                </h3>
                """)
            .mandatory(PCSCase::getIsCorrectContactEmail)
            .mandatory(PCSCase::getUpdatedContactEmail, "isCorrectContactEmail=\"NO\"")

            // Address section
            .label("contactPreferences-info-address", """
                ----
                <h2 class="govuk-heading-m">Correspondence address</h2>
                <p class="govuk-body-m">
                    Court documents like orders and notices will be sent by post to the address registered with
                    My HMCTS.<br><br>
                    You can change this correspondence address if, for example, you work in a different office from
                    the address registered with My HMCTS.
                </p>
                """)
            .readonly(PCSCase::getFormattedContactAddress, NEVER_SHOW)
            .label("contactAddressSummaryLabel", """
                <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                    Your My HMCTS registered address is:
                </h3>
                <p class="govuk-body-s govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    ${formattedContact Address}
                </p>
                """)
            .label("contactPreferences-address-question", """
                <h3 class="govuk-heading-m govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    Do you want documents to be sent to this address?
                </h3>
                """)
            .mandatory(PCSCase::getIsCorrectContactAddress)
            .mandatory(PCSCase::getUpdatedContactAddress, "isCorrectContactAddress=\"NO\"")

            // Phone section
            .label("contactPreferences-info-phoneNumber", """
                ----
                <h2 class="govuk-heading-m">Contact phone number</h2>
                <p class="govuk-body-m govuk-!-margin-bottom-1">
                    You should provide a phone number so we can contact you if there are urgent updates.
                </p>
                <h3 class="govuk-heading-m govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    Do you want to provide a contact phone number?(Optional)
                </h3>
                """)
            .mandatory(PCSCase::getProvidePhoneNumber)
            .mandatory(PCSCase::getContactPhoneNumber, "providePhoneNumber=\"YES\"")
            .label("contactPreferences-info-separator", "---");
    }

}

