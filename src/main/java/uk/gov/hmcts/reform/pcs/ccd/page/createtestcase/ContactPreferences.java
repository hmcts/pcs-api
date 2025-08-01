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
            .readonly(PCSCase::getClaimantContactEmail, NEVER_SHOW)
            .label("contactPreferences-email", """
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
                    ${claimantContactEmail}
                </p>
                """)
            .label("contactPreferences-email-question", """
                <h3 class="govuk-heading-m govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    Do you want to use this email address for notifications?
                </h3>
                """)
            .mandatory(PCSCase::getIsCorrectClaimantContactEmail)
            .mandatory(PCSCase::getOverriddenClaimantContactEmail, "isCorrectClaimantContactEmail=\"NO\"")

            // Address section
            .label("contactPreferences-address-info", """
                ----
                <h2 class="govuk-heading-m">Correspondence address</h2>
                <p class="govuk-body-m">
                    Court documents like orders and notices will be sent by post to the address registered with
                    My HMCTS.<br><br>
                    You can change this correspondence address if, for example, you work in a different office from
                    the address registered with My HMCTS.
                </p>
                """)
            .readonly(PCSCase::getFormattedClaimantContactAddress, NEVER_SHOW)
            .label("contactPreferences-address-registered", """
                <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                    Your My HMCTS registered address is:
                </h3>
                <p class="govuk-body-s govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    ${formattedClaimantContactAddress}
                </p>
                """)
            .label("contactPreferences-address-question", """
                <h3 class="govuk-heading-m govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    Do you want documents to be sent to this address?
                </h3>
                """)
            .mandatory(PCSCase::getIsCorrectClaimantContactAddress)
            .mandatory(PCSCase::getOverriddenClaimantContactAddress, "isCorrectClaimantContactAddress=\"NO\"")

            // Phone section
            .label("contactPreferences-phoneNumber-question", """
                ----
                <h2 class="govuk-heading-m">Contact phone number</h2>
                <p class="govuk-body-m govuk-!-margin-bottom-1">
                    You should provide a phone number so we can contact you if there are urgent updates.
                </p>
                <h3 class="govuk-heading-m govuk-!-margin-top-1 govuk-!-margin-bottom-1">
                    Do you want to provide a contact phone number?(Optional)
                </h3>
                """)
            .mandatory(PCSCase::getClaimantProvidePhoneNumber)
            .mandatory(PCSCase::getClaimantContactPhoneNumber, "claimantProvidePhoneNumber=\"YES\"")
            .label("contactPreferences-phoneNumber-separator", "---");
    }

}

