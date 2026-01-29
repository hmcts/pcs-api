package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.UnderlesseeMortgageeValidator;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class UnderlesseeOrMortgageeDetailsPage implements CcdPageConfiguration {

    private final UnderlesseeMortgageeValidator underlesseeMortgageeValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("underlesseeMortgageeDetails", this::midEvent)
            .pageLabel("Underlessee or mortgagee details")
            .showCondition("hasUnderlesseeOrMortgagee=\"YES\"")
            .complex(PCSCase::getUnderlesseeOrMortgagee1)
                .readonlyNoSummary(UnderlesseeMortgageeDetails::getNameSectionLabel)
                .mandatory(UnderlesseeMortgageeDetails::getNameKnown)
                .mandatory(UnderlesseeMortgageeDetails::getName)
                .readonlyNoSummary(UnderlesseeMortgageeDetails::getAddressSectionLabel)
                .mandatory(UnderlesseeMortgageeDetails::getAddressKnown)
                    .complex(UnderlesseeMortgageeDetails::getAddress)
                        .mandatory(AddressUK::getAddressLine1)
                        .optional(AddressUK::getAddressLine2)
                        .optional(AddressUK::getAddressLine3)
                        .mandatory(AddressUK::getPostTown)
                        .optional(AddressUK::getCounty)
                        .optional(AddressUK::getCountry)
                        .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                    .done()
            .done()
            .label("underlesseeMortgagee-add-additional", """
                ---
                <h2 class="govuk-heading-m">Additional underlessees or mortgagees</h2>
                """)
            .mandatory(PCSCase::getAddAdditionalUnderlesseeOrMortgagee)
            .list(PCSCase::getAdditionalUnderlesseeOrMortgagee,
                       "addAdditionalUnderlesseeOrMortgagee=\"YES\"")
                .readonlyNoSummary(UnderlesseeMortgageeDetails::getNameSectionLabel)
                .mandatory(UnderlesseeMortgageeDetails::getNameKnown)
                .mandatory(UnderlesseeMortgageeDetails::getName)
                .readonlyNoSummary(UnderlesseeMortgageeDetails::getAddressSectionLabel)
                .mandatory(UnderlesseeMortgageeDetails::getAddressKnown)
                .complex(UnderlesseeMortgageeDetails::getAddress)
                .mandatory(AddressUK::getAddressLine1)
                .optional(AddressUK::getAddressLine2)
                .optional(AddressUK::getAddressLine3)
                .mandatory(AddressUK::getPostTown)
                .optional(AddressUK::getCounty)
                .optional(AddressUK::getCountry)
                .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
            .done()
            .label("underlesseeMortgagee-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        boolean additionalUnderlesseeMortgagee = caseData.getAddAdditionalUnderlesseeOrMortgagee() == VerticalYesNo.YES;

        UnderlesseeMortgageeDetails underlesseeMortgagee1 = caseData.getUnderlesseeOrMortgagee1();
        List<String> validationErrors =
            new ArrayList<>(underlesseeMortgageeValidator.validateUnderlesseeOrMortgagee1(
                                                                underlesseeMortgagee1,
                                                                additionalUnderlesseeMortgagee
                                                            ));

        if (additionalUnderlesseeMortgagee) {
            validationErrors
                .addAll(underlesseeMortgageeValidator.validateAdditionalUnderlesseeOrMortgagee(
                    caseData.getAdditionalUnderlesseeOrMortgagee()));
        }

        if (!validationErrors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(validationErrors)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();

    }
}
