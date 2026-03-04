package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.ShowCondition;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.NEVER_SHOW;

@AllArgsConstructor
@Component
public class ContactPreferences implements CcdPageConfiguration {

    private final AddressValidator addressValidator;
    private final TextAreaValidationService textAreaValidationService;
    private static final String EMAIL_LABEL = "Enter email address";

    private static final ShowCondition INCORRECT_EMAIL_ADDRESS = when(
        ClaimantContactPreferences::getIsCorrectClaimantContactEmail)
        .is(VerticalYesNo.NO);
    private static final ShowCondition ORG_ADDRESS_FOUND = when(
        ClaimantContactPreferences::getOrgAddressFound)
        .is(YesOrNo.YES);
    private static final ShowCondition ORG_ADDRESS_NOT_FOUND = when(
        ClaimantContactPreferences::getOrgAddressFound)
        .is(YesOrNo.NO);
    private static final ShowCondition INCORRECT_ADDRESS = when(
        ClaimantContactPreferences::getIsCorrectClaimantContactAddress)
        .is(VerticalYesNo.NO);
    private static final ShowCondition PROVIDE_PHONE_NUMBER = when(
        ClaimantContactPreferences::getClaimantProvidePhoneNumber)
        .is(VerticalYesNo.YES);

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("contactPreferences", this::midEvent)
            .pageLabel("Contact preferences")

            // Email section
            .complex(PCSCase::getClaimantContactPreferences)
                .readonly(ClaimantContactPreferences::getClaimantContactEmail, NEVER_SHOW)
            .label("contactPreferences-email", """
                    ---
                    <h2 class="govuk-heading-m">Notifications</h2>
                    <p class="govuk-body-m govuk-!-margin-bottom-1">
                        You’ll receive updates about your claim by email. For example, when a hearing
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
            .mandatoryWhen(ClaimantContactPreferences::getOverriddenClaimantContactEmail, INCORRECT_EMAIL_ADDRESS)
            .done()

            // Address section
            .complex(PCSCase::getClaimantContactPreferences)
            .readonly(ClaimantContactPreferences::getOrgAddressFound, NEVER_SHOW)
            // Address found
            .readonly(ClaimantContactPreferences::getOrganisationAddress, NEVER_SHOW, true)
            .readonly(ClaimantContactPreferences::getFormattedClaimantContactAddress, NEVER_SHOW)
            .labelWhen("contactPreferences-address-info-yes", """
                    ----
                    <h2 class="govuk-heading-m">Correspondence address</h2>
                    <p class="govuk-body-m">
                        Court documents like orders and notices will be sent by post to the address registered with
                        My HMCTS.<br><br>
                        You can change this correspondence address if, for example, you work in a different office from
                        the address registered with My HMCTS.
                    </p>
                    """, ORG_ADDRESS_FOUND)
            .labelWhen("contactPreferences-address-registered", """
                    <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                        Your organisation’s My HMCTS registered address is:
                    </h3>
                    <p class="govuk-body-s govuk-!-margin-top-1">
                        ${formattedClaimantContactAddress}
                    </p>
                    """, ORG_ADDRESS_FOUND)
            .mandatoryWhen(ClaimantContactPreferences::getIsCorrectClaimantContactAddress, ORG_ADDRESS_FOUND)

            // Address not found
            .labelWhen("contactPreferences-address-info-no", """
                ----
                <h2 class="govuk-heading-m">Correspondence address</h2>
                <p class="govuk-body-m">
                    Court documents like orders and notices will be sent by post to the address registered with
                    My HMCTS.
                </p>
                """, ORG_ADDRESS_NOT_FOUND)
            .labelWhen("contactPreferences-address-missing", """
                    <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                        We could not retrieve your organisation’s correspondence address that’s linked to your My
                        HMCTS account
                    </h3>
                    <p class="govuk-hint govuk-!-margin-top-1">
                        You must enter the correspondence address you’d like to receive documents to
                    </p>
                    """, ORG_ADDRESS_NOT_FOUND)

            // Rest of address
            .complexWhen(
                ClaimantContactPreferences::getOverriddenClaimantContactAddress,
                INCORRECT_ADDRESS.or(ORG_ADDRESS_NOT_FOUND)
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
            .complex(PCSCase::getClaimantContactPreferences)
                .label("contactPreferences-phoneNumber-question", """
                    ----
                    <h2 class="govuk-heading-m">Contact phone number</h2>
                    <p class="govuk-body-m ">
                        You should provide a phone number so we can contact you if there are urgent updates.
                    </p>
                    """)
                .optional(ClaimantContactPreferences::getClaimantProvidePhoneNumber)
                .mandatoryWhen(ClaimantContactPreferences::getClaimantContactPhoneNumber, PROVIDE_PHONE_NUMBER)
            .done()
            .label("contactPreferences-phoneNumber-separator", "---")
            .label("contactPreferences-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> validationErrors = new ArrayList<>();
        ClaimantContactPreferences contactPreferences = caseData.getClaimantContactPreferences();

        if (contactPreferences != null) {
            VerticalYesNo isCorrectClaimantContactAddress = contactPreferences.getIsCorrectClaimantContactAddress();
            if (isCorrectClaimantContactAddress == VerticalYesNo.NO
                || contactPreferences.getOrgAddressFound() == YesOrNo.NO) {
                AddressUK contactAddress = contactPreferences.getOverriddenClaimantContactAddress();
                validationErrors.addAll(addressValidator.validateAddressFields(contactAddress));

            }
            String overriddenEmail = contactPreferences.getOverriddenClaimantContactEmail();
            VerticalYesNo isCorrectEmailAddress = contactPreferences.getIsCorrectClaimantContactEmail();
            if (isCorrectEmailAddress == VerticalYesNo.NO && overriddenEmail != null) {
                validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                    overriddenEmail, EMAIL_LABEL, TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT)
                );
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", validationErrors))
            .data(caseData)
            .build();

    }

}
