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
                clearCrossBorderLabels(caseData);
            }
            case NO_MATCH_FOUND -> {
                log.debug("No court found for postcode: {}", postcode);
                caseData.setShowCrossBorderPage(YesOrNo.NO);
                caseData.setShowPropertyNotEligiblePage(YesOrNo.NO);
                caseData.setShowPostcodeNotAssignedToCourt(YesOrNo.YES);
                caseData.setPostcodeNotAssignedView("ALL_COUNTRIES");
                caseData.setLegislativeCountry(null);
                clearCrossBorderLabels(caseData);
            }
            case MULTIPLE_MATCHES_FOUND -> {
                // TODO: HDPI-1838 will handle multiple matches
                throw new UnsupportedOperationException("TODO: Not yet implemented");
            }
            case ELIGIBLE -> {
                caseData.setShowCrossBorderPage(YesOrNo.NO);
                caseData.setShowPropertyNotEligiblePage(YesOrNo.NO);
                caseData.setShowPostcodeNotAssignedToCourt(YesOrNo.NO);
                caseData.setLegislativeCountry(eligibilityResult.getLegislativeCountry());
                clearCrossBorderLabels(caseData);
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
        String country1Label = crossBorderCountries.get(0).getLabel();
        String country2Label = crossBorderCountries.get(1).getLabel();
        caseData.setCrossBorderCountry1(country1Label);
        caseData.setCrossBorderCountry2(country2Label);
        
        // Build the label texts dynamically in midEvent so they're correct when the page is rendered
        // The label template interpolation happens when the page is shown, before CrossBorderPostcodeSelection
        // midEvent runs, so we must build these labels here
        String infoLabel = buildCrossBorderInfoLabel(country1Label, country2Label);
        String questionLabel = buildCrossBorderQuestionLabel(country1Label, country2Label);
        
        caseData.setCrossBorderInfoLabel(infoLabel);
        caseData.setCrossBorderQuestionLabel(questionLabel);
        
        log.debug("Set up cross-border data for postcode {}: {} and {}",
            caseData.getPropertyAddress().getPostCode(),
            country1Label,
            country2Label);
    }

    private String buildCrossBorderInfoLabel(String country1, String country2) {
        return """
            ---
            <section tabindex="0">
            <p class="govuk-body">
            Your postcode includes properties in %s and %s. We need to know
            which country your property is in, as the law is different in each country.
            </p>

            <p class="govuk-body">
            If you're not sure which country your property is in, try searching for your
            address on the land and property register.
            </p>

            <div class="govuk-warning-text" role="alert" aria-labelledby="warning-message">
              <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
              <strong class="govuk-warning-text__text">
                <span class="govuk-warning-text__assistive">Warning</span>
                <span id="warning-message">
                  Your case could be delayed or rejected if you select the wrong country.
                </span>
              </strong>
            </div>
            </section>
            """.formatted(country1, country2);
    }

    private String buildCrossBorderQuestionLabel(String country1, String country2) {
        return "Is the property located in %s or %s?".formatted(country1, country2);
    }

    /**
     * Clears cross-border labels when the postcode is no longer cross-border.
     * This ensures stale labels don't persist when postcode changes from cross-border to non-cross-border.
     */
    private void clearCrossBorderLabels(PCSCase caseData) {
        caseData.setCrossBorderCountry1(null);
        caseData.setCrossBorderCountry2(null);
        caseData.setCrossBorderInfoLabel(null);
        caseData.setCrossBorderQuestionLabel(null);
        caseData.setCrossBorderCountriesList(null);
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
