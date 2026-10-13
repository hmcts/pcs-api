package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantValidator;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class DefendantsDetails implements CcdPageConfiguration {

    private final DefendantValidator defendantValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDetails", this::midEvent)
            .pageLabel("Defendant details")
            .complex(PCSCase::getDefendant1)
                .readonly(DefendantDetails::getNameSectionLabel)
                .mandatory(DefendantDetails::getNameKnown)
                .mandatory(DefendantDetails::getFirstName)
                .mandatory(DefendantDetails::getLastName)

                .readonly(DefendantDetails::getAddressSectionLabel)
                .mandatory(DefendantDetails::getAddressKnown)
                .mandatory(DefendantDetails::getAddressSameAsPossession)
                .complex(DefendantDetails::getCorrespondenceAddress)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
            .done()
            .label("defendantsDetails-additionalDefendants", """
                ---
                <h2>Additional defendants</h2>""")
            .mandatory(PCSCase::getAddAnotherDefendant)
            .list(PCSCase::getAdditionalDefendants, "addAnotherDefendant=\"YES\"")
                .readonly(DefendantDetails::getNameSectionLabel)
                .mandatory(DefendantDetails::getNameKnown)
                .mandatory(DefendantDetails::getFirstName)
                .mandatory(DefendantDetails::getLastName)
                .readonly(DefendantDetails::getAddressSectionLabel)
                .mandatory(DefendantDetails::getAddressKnown)
                .mandatory(DefendantDetails::getAddressSameAsPossession)
                .complex(DefendantDetails::getCorrespondenceAddress)
                .mandatory(AddressUK::getAddressLine1)
                .optional(AddressUK::getAddressLine2)
                .optional(AddressUK::getAddressLine3)
                .mandatory(AddressUK::getPostTown)
                .optional(AddressUK::getCounty)
                .optional(AddressUK::getCountry)
                .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
            .done()
            .label("defendantsDetails-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        boolean additionalDefendantsProvided = caseData.getAddAnotherDefendant() == VerticalYesNo.YES;

        DefendantDetails defendantDetails = caseData.getDefendant1();
        List<String> validationErrors
            = new ArrayList<>(defendantValidator.validateDefendant1(defendantDetails, additionalDefendantsProvided));

        if (additionalDefendantsProvided) {
            validationErrors
                .addAll(defendantValidator.validateAdditionalDefendants(caseData.getAdditionalDefendants()));
        }

        if (!validationErrors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", validationErrors))
                .build();
        }

        DefendantCircumstances defendantCircumstances = caseData.getDefendantCircumstances();
        defendantCircumstances.setDefendantTermPossessive(additionalDefendantsProvided ? "defendants’" : "defendant’s");
        showSavedCircumstancesForEitherDefendantCount(defendantCircumstances);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();

    }

    private static void showSavedCircumstancesForEitherDefendantCount(DefendantCircumstances circumstances) {
        circumstances.setHasSingleDefendantCircumstancesInfo(circumstances.getHasDefendantCircumstancesInfo());
        circumstances.setSingleDefendantCircumstancesInfo(circumstances.getDefendantCircumstancesInfo());
        circumstances.setHasMultipleDefendantsCircumstancesInfo(circumstances.getHasDefendantCircumstancesInfo());
        circumstances.setMultipleDefendantsCircumstancesInfo(circumstances.getDefendantCircumstancesInfo());
    }

}
