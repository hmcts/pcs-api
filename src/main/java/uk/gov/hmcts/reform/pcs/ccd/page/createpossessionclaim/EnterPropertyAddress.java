package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.EligibilityCheckException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
@Slf4j
public class EnterPropertyAddress implements CcdPageConfiguration {

    private final EligibilityService eligibilityService;
    private final AddressValidator addressValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enterPropertyAddress", this::midEvent)
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("enterPropertyAddress-lineSeparator", "---")
            .complex(PCSCase::getPropertyAddress)
                .mandatory(AddressUK::getAddressLine1)
                .optional(AddressUK::getAddressLine2)
                .optional(AddressUK::getAddressLine3)
                .mandatory(AddressUK::getPostTown)
                .optional(AddressUK::getCounty)
                .optional(AddressUK::getCountry)
                .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
            .done()
            .readonly(PCSCase::getLegislativeCountry, NEVER_SHOW, true);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        AddressUK propertyAddress = caseData.getPropertyAddress();

        List<String> validationErrors = addressValidator.validateAddressFields(propertyAddress);
        if (!validationErrors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(validationErrors)
                .build();
        }

        String postcode = propertyAddress.getPostCode();
        EligibilityResult eligibilityResult = eligibilityService.checkEligibility(postcode, null);
        log.debug("EnterPropertyAddress eligibility check: {} for postcode {} with countries {}",
            eligibilityResult.getStatus(), postcode, eligibilityResult.getLegislativeCountries());

        switch (eligibilityResult.getStatus()) {
            case LEGISLATIVE_COUNTRY_REQUIRED -> {
                validateLegislativeCountries(eligibilityResult.getLegislativeCountries(), postcode);
                setupCrossBorderData(caseData, eligibilityResult.getLegislativeCountries());
            }
            case NOT_ELIGIBLE -> {
                caseData.setShowCrossBorderPage(YesOrNo.NO);
                caseData.setShowPropertyNotEligiblePage(YesOrNo.YES);
                caseData.setLegislativeCountry(eligibilityResult.getLegislativeCountry());
            }
            case NO_MATCH_FOUND -> {
                log.debug("No court found for postcode: {}", postcode);
                caseData.setShowPostcodeNotAssignedToCourt(YesOrNo.YES);
                caseData.setPostcodeNotAssignedView("ALL_COUNTRIES");
                caseData.setShowCrossBorderPage(YesOrNo.NO);
            }
            case MULTIPLE_MATCHES_FOUND -> {
                // TODO: HDPI-1838 will handle multiple matches
                throw new UnsupportedOperationException("TODO: Not yet implemented");
            }
            case ELIGIBLE -> {
                caseData.setShowCrossBorderPage(YesOrNo.NO);
                caseData.setShowPostcodeNotAssignedToCourt(YesOrNo.NO);
                caseData.setLegislativeCountry(eligibilityResult.getLegislativeCountry());
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private void validateLegislativeCountries(List<LegislativeCountry> legislativeCountries, String postcode) {
        if (legislativeCountries == null || legislativeCountries.size() < 2) {
            throw new EligibilityCheckException(String.format(
                "Expected at least 2 legislative countries when status is LEGISLATIVE_COUNTRY_REQUIRED, "
                    + "but got %d for postcode: %s",
                legislativeCountries == null ? 0 : legislativeCountries.size(),
                postcode
            ));
        }
    }

    private void setupCrossBorderData(PCSCase caseData, List<LegislativeCountry> legislativeCountries) {
        caseData.setShowCrossBorderPage(YesOrNo.YES);

        List<DynamicStringListElement> crossBorderCountries =
            createCrossBorderCountriesList(legislativeCountries);
        DynamicStringList crossBorderCountriesList = DynamicStringList.builder()
            .listItems(crossBorderCountries)
            .build();

        caseData.setCrossBorderCountriesList(crossBorderCountriesList);
        // Set individual cross border countries
        caseData.setCrossBorderCountry1(crossBorderCountries.get(0).getLabel());
        caseData.setCrossBorderCountry2(crossBorderCountries.get(1).getLabel());
    }

    private List<DynamicStringListElement> createCrossBorderCountriesList(
        List<LegislativeCountry> legislativeCountries) {
        return legislativeCountries.stream()
            .map(value -> DynamicStringListElement.builder()
                .code(value.name())
                .label(value.getLabel())
                .build())
            .toList();
    }
}
