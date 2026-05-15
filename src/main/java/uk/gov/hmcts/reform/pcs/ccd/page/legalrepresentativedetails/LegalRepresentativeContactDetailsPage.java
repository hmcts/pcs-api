package uk.gov.hmcts.reform.pcs.ccd.page.legalrepresentativedetails;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.LegalRepresentativeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
@AllArgsConstructor
public class LegalRepresentativeContactDetailsPage implements CcdPageConfiguration {

    private static final String ORG_ADDRESS_FOUND = "orgAddressFound=\"Yes\"";
    private static final String ORG_ADDRESS_NOT_FOUND = "orgAddressFound=\"No\"";
    private static final String EMAIL_LABEL = "Enter email address";

    private final AddressValidator addressValidator;
    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("legalRepresentativeContactDetails", this::midEvent)
            .pageLabel("Amend representative's details")
            .complex(PCSCase::getLegalRepresentativeContactDetails)
            .label("legalRepresentativeDetails-reference",  """
                    ---
                    <h2 class="govuk-heading-m">Reference</h2>
                    <p class="govuk-body-m govuk-!-margin-bottom-1">
                        You should provide a reference number.
                    </p>

                    """)

            .optional(LegalRepresentativeDetails::getReference)
            .readonly(LegalRepresentativeDetails::getOriginalEmailAddress, NEVER_SHOW)
            .label("legalRepresentativeDetails-email",  """
                    ---
                    <h2 class="govuk-heading-m">Notifications</h2>
                    <p class="govuk-body-m govuk-!-margin-bottom-2">
                        You’ll receive updates about your claim by email. For example, when a hearing
                        has been scheduled or when a document is ready to view.
                    </p>
                    <p class="govuk-body-m govuk-!-margin-bottom-1 govuk-!-font-weight-bold">
                        Your My HMCTS registered email address is:
                    </p>
                    <p class="govuk-body-m govuk-!-margin-bottom-2">
                       ${originalEmailAddress}
                    </p>
                    """)
            .mandatory(LegalRepresentativeDetails::getUseEmailAddress)
            .mandatory(LegalRepresentativeDetails::getEmailAddress, "useEmailAddress=\"NO\"")
            .readonly(LegalRepresentativeDetails::getOrgAddressFound, NEVER_SHOW)
            .readonly(LegalRepresentativeDetails::getOrganisationAddress, NEVER_SHOW, true)
            .readonly(LegalRepresentativeDetails::getFormattedClaimantContactAddress, NEVER_SHOW)
            .label("legalRepresentativeDetails-address-info-yes", """
                    ----
                    <h2 class="govuk-heading-m">Service address</h2>
                    <p class="govuk-body-m">
                        Court documents like orders and notices will be sent by post to the address registered with
                        My HMCTS.<br><br>
                        You can change this service address if, for example, you work in a different office from
                        the address registered with My HMCTS.
                    </p>
                    """, ORG_ADDRESS_FOUND)
            .label("contactPreferences-address-registered", """
                    <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                        Your My HMCTS registered address is:
                    </h3>
                    <p class="govuk-body-s govuk-!-margin-top-1">
                        ${formattedClaimantContactAddress}
                    </p>
                    """, ORG_ADDRESS_FOUND)

            // Address not found
            .label("legalRepresentativeDetails-address-info-no", """
                ----
                <h2 class="govuk-heading-m">Service address</h2>
                <p class="govuk-body-m">
                    Court documents like orders and notices will be sent by post to the address registered with
                    My HMCTS.
                </p>
                <p class="govuk-body-m">
                    You can change this service address if, for example, you work in a different address with My HMCTS.
                </p>
                """, ORG_ADDRESS_NOT_FOUND)
            .label("contactPreferences-address-missing", """
                    <h3 class="govuk-heading-m govuk-!-margin-bottom-1">
                        We could not retrieve your service address that’s linked to your My
                        HMCTS account
                    </h3>
                    <p class="govuk-hint govuk-!-margin-top-1">
                        You must enter the service address you’d like to receive documents to
                    </p>
                    """, ORG_ADDRESS_NOT_FOUND)
            .mandatory(LegalRepresentativeDetails::getDifferentPostalAddress)
            .complex(LegalRepresentativeDetails::getCorrespondenceAddress, "differentPostalAddress=\"YES\"")
            .mandatory(AddressUK::getAddressLine1)
            .optional(AddressUK::getAddressLine2)
            .optional(AddressUK::getAddressLine3)
            .mandatory(AddressUK::getPostTown)
            .optional(AddressUK::getCounty)
            .optional(AddressUK::getCountry)
            .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
            .done()
            .label("legalRepresentativeDetails-phoneNumber-question", """
                    ----
                    <h2 class="govuk-heading-m">Contact phone number</h2>
                    <p class="govuk-body-m ">
                        You should provide a phone number so we can contact you if there are urgent updates.
                    </p>
                    """)
                .optional(LegalRepresentativeDetails::getProvideContactPhoneNumber)
                .mandatory(LegalRepresentativeDetails::getContactPhoneNumber, "provideContactPhoneNumber=\"YES\"")
            .done()
            .label("legalRepresentativeDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<String> validationErrors = new ArrayList<>();
        LegalRepresentativeDetails legalRepresentativeDetails = caseData.getLegalRepresentativeContactDetails();

        if (legalRepresentativeDetails != null) {

            if (legalRepresentativeDetails.getDifferentPostalAddress() == VerticalYesNo.YES
                || legalRepresentativeDetails.getOrgAddressFound() == YesOrNo.NO) {
                AddressUK contactAddress = legalRepresentativeDetails.getCorrespondenceAddress();
                validationErrors.addAll(addressValidator.validateAddressFields(contactAddress));

            }

            if (legalRepresentativeDetails.getUseEmailAddress() == VerticalYesNo.NO ) {
                validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                    legalRepresentativeDetails.getEmailAddress(), EMAIL_LABEL, TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT)
                );
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", validationErrors))
            .data(caseData)
            .build();

    }

}
