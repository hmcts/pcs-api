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
import uk.gov.hmcts.reform.pcs.ccd.service.UnderlesseeMortgageeValidator;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class UnderlesseeMortgageeDetailsPage implements CcdPageConfiguration {

    private final UnderlesseeMortgageeValidator underlesseeMortgageeValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("underlesseeMortgageeDetails", this::midEvent)
            .pageLabel("Underlessee or mortgagee details")
            .showCondition("hasUnderlesseeOrMortgagee=\"YES\"")
            .complex(PCSCase::getUnderlesseeMortgagee1)
                .readonly(UnderlesseeMortgageeDetails::getUnderlesseeOrMortgageeNameLabel)
                .mandatory(UnderlesseeMortgageeDetails::getUnderlesseeOrMortgageeNameKnown)
                .mandatory(UnderlesseeMortgageeDetails::getUnderlesseeOrMortgageeName)
                .readonly(UnderlesseeMortgageeDetails::getUnderlesseeOrMortgageeAddressLabel)
                .mandatory(UnderlesseeMortgageeDetails::getUnderlesseeOrMortgageeAddressKnown)
                    .complex(UnderlesseeMortgageeDetails::getUnderlesseeOrMortgageeAddress)
                        .mandatory(AddressUK::getAddressLine1)
                        .optional(AddressUK::getAddressLine2)
                        .optional(AddressUK::getAddressLine3)
                        .mandatory(AddressUK::getPostTown)
                        .optional(AddressUK::getCounty)
                        .optional(AddressUK::getCountry)
                        .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                    .done()
            .done()
            .label("underlesseeMortgagee-additional", """
                ---
                <h2 class="govuk-heading-m">Additional underlessee or mortgagee?</h2>
                """)
            .mandatory(PCSCase::getAddAdditionalUnderlesseeOrMortgagee)
            .mandatory(PCSCase::getAdditionalUnderlesseeMortgagee,
                       "addAdditionalUnderlesseeOrMortgagee=\"YES\"");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        boolean additionalUnderlesseeMortgagee = caseData.getAddAdditionalUnderlesseeOrMortgagee() == VerticalYesNo.YES;

        UnderlesseeMortgageeDetails underlesseeMortgagee1 = caseData.getUnderlesseeMortgagee1();
        List<String> validationErrors =
            new ArrayList<>(underlesseeMortgageeValidator.validateUnderlesseeOrMortgagee1(
                                                                underlesseeMortgagee1,
                                                                additionalUnderlesseeMortgagee
                                                            ));

        if (additionalUnderlesseeMortgagee) {
            validationErrors
                .addAll(underlesseeMortgageeValidator.validateAdditionalUnderlesseeOrMortgagee(
                    caseData.getAdditionalUnderlesseeMortgagee()));
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
