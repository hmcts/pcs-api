package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class TenancyLicenceDetailsPage implements CcdPageConfiguration {

    private final Clock ukClock;
    private final TextAreaValidationService textAreaValidationService;

    public TenancyLicenceDetailsPage(@Qualifier("ukClock") Clock ukClock,
                                TextAreaValidationService textAreaValidationService) {
        this.ukClock = ukClock;
        this.textAreaValidationService = textAreaValidationService;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("tenancyLicenceDetails", this::midEvent)
            .pageLabel("Tenancy or licence details")
            .showCondition("legislativeCountry=\"England\"")
            .label("tenancyLicenceDetails-info", """
               ---
               <h2 class="govuk-heading-m">Tenancy or licence type</h2>
               """)
            .complex(PCSCase::getTenancyLicenceDetails)
                .mandatory(
                    TenancyLicenceDetails::getTypeOfTenancyLicence
                )
                .mandatory(
                    TenancyLicenceDetails::getDetailsOfOtherTypeOfTenancyLicence,
                    "tenancy_TypeOfTenancyLicence=\"OTHER\""
                )
            .done()
            .label("tenancyLicenceDetails-date-section", """
               ---
               <h2 class="govuk-heading-m">Tenancy or licence start date</h2>
               """)
            .complex(PCSCase::getTenancyLicenceDetails)
                .optional(TenancyLicenceDetails::getTenancyLicenceDate)
            .done()
            .label("tenancyLicenceDetails-doc-section", """
               ---
               <h2 class="govuk-heading-m">Upload tenancy or licence agreement</h2>
               <h3>Do you want to upload a copy of the tenancy or licence agreement?
               (Optional)</h3>
               <p class="govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1">
                You can either upload this now or closer to the hearing date. Any documents you upload now will be
                included in the pack of documents a judge will receive before hearing the hearing (the bundle)
                </p>
               """)
            .complex(PCSCase::getTenancyLicenceDetails)
                .optional(TenancyLicenceDetails::getTenancyLicenceDocuments)
            .done()
            .label("lineSeparator", "---")
            .label("tenancyLicenceDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        LocalDate tenancyLicenceDate = caseData.getTenancyLicenceDetails() != null
                ? caseData.getTenancyLicenceDetails().getTenancyLicenceDate() : null;
        LocalDate currentDate = LocalDate.now(ukClock);

        // Validate tenancy licence date
        if (tenancyLicenceDate != null && !tenancyLicenceDate.isBefore(currentDate)) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of("Date the tenancy or licence began must be in the past"))
            .build();
        }

        // Validate details of other type of tenancy licence character limit
        List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
            caseData.getTenancyLicenceDetails() != null
                    ? caseData.getTenancyLicenceDetails().getDetailsOfOtherTypeOfTenancyLicence() : null,
            TenancyLicenceDetails.DETAILS_OF_OTHER_TYPE_OF_TENANCY_LICENCE_LABEL,
            TextAreaValidationService.MEDIUM_TEXT_LIMIT
        );

        if (!validationErrors.isEmpty()) {
            return textAreaValidationService.createValidationResponse(caseData, validationErrors);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}


