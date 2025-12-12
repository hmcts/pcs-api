package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class ContactPreferences implements CcdPageConfiguration {

    private final AddressValidator addressValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("contactPreferences", this::midEvent)
            .pageLabel("Contact preferences")

            // Email section
            .complex(PCSCase::getContactPreferencesDetails)
                .readonly(ClaimantContactPreferences::getClaimantContactEmail, NEVER_SHOW)
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
                    <p class="govuk-body-s govuk-!-margin-top-1">
                        ${claimantContactEmail}
                    </p>
                    """)
                .mandatory(ClaimantContactPreferences::getIsCorrectClaimantContactEmail)
                .mandatory(
                    ClaimantContactPreferences::getOverriddenClaimantContactEmail,
                    "isCorrectClaimantContactEmail=\"NO\""
                )
            .done()

            // Address section
            .complex(PCSCase::getContactPreferencesDetails)
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
                .readonly(ClaimantContactPreferences::getFormattedClaimantContactAddress, NEVER_SHOW)
                .label("contactPreferences-address-registered", """
                    <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                        Your My HMCTS registered address is:
                    </h3>
                    <p class="govuk-body-s govuk-!-margin-top-1">
                        ${formattedClaimantContactAddress}
                    </p>
                    """)
                .mandatory(ClaimantContactPreferences::getIsCorrectClaimantContactAddress)
                .complex(
                    ClaimantContactPreferences::getOverriddenClaimantContactAddress,
                    "isCorrectClaimantContactAddress=\"NO\""
                )
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
            .done()

            // Phone section
            .complex(PCSCase::getContactPreferencesDetails)
                .label("contactPreferences-phoneNumber-question", """
                    ----
                    <h2 class="govuk-heading-m">Contact phone number</h2>
                    <p class="govuk-body-m ">
                        You should provide a phone number so we can contact you if there are urgent updates.
                    </p>
                    """)
                .optional(ClaimantContactPreferences::getClaimantProvidePhoneNumber)
                .mandatory(
                    ClaimantContactPreferences::getClaimantContactPhoneNumber,
                    "claimantProvidePhoneNumber=\"YES\""
                )
            .done()
            .label("contactPreferences-phoneNumber-separator", "---")
            .label("contactPreferences-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        ClaimantContactPreferences contactPreferences = caseData.getContactPreferencesDetails();
        if (contactPreferences != null) {
            VerticalYesNo isCorrectClaimantContactAddress = contactPreferences.getIsCorrectClaimantContactAddress();
            if (isCorrectClaimantContactAddress == VerticalYesNo.NO) {
                AddressUK contactAddress = contactPreferences.getOverriddenClaimantContactAddress();
                List<String> validationErrors = addressValidator.validateAddressFields(contactAddress);
                if (!validationErrors.isEmpty()) {
                    return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                        .errors(validationErrors)
                        .build();
                }
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();

    }

}

