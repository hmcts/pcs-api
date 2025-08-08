package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.EligibilityCheckException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.util.List;

@AllArgsConstructor
@Component
@Slf4j
public class MakeAClaim implements CcdPageConfiguration {

    private final EligibilityService eligibilityService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Make a claim", this::midEvent)
            .pageLabel(
                "What is the address of the property you're claiming "
                    + "possession of?"
            )
            .label("lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> detailsBefore
    ) {
        log.info("MakeAClaim midEvent started for case ID: {}", details.getId());

        PCSCase caseData = details.getData();
        String postcode = caseData.getPropertyAddress().getPostCode();

        log.debug("Processing MakeAClaim for postcode: {}", postcode);

        log.info("Performing initial eligibility check for postcode: {}", postcode);
        EligibilityResult eligibilityResult =
            eligibilityService.checkEligibility(postcode, null);

        log.debug(
            "Initial eligibility check completed - Status: {}, Legislative Countries: {}",
            eligibilityResult.getStatus(),
            eligibilityResult.getLegislativeCountries()
        );

        // Reset flags every run
        caseData.setShowCrossBorderPage(YesOrNo.NO);
        caseData.setShowPropertyNotEligiblePage(YesOrNo.NO);
        log.debug("Reset eligibility flags - showCrossBorderPage: NO, showPropertyNotEligiblePage: NO");

        switch (eligibilityResult.getStatus()) {
            case LEGISLATIVE_COUNTRY_REQUIRED -> {
                log.info("MakeAClaim eligibility check: LEGISLATIVE_COUNTRY_REQUIRED for postcode {}. "
                        + "Setting up cross-border selection", postcode);
                validateLegislativeCountries(
                    eligibilityResult.getLegislativeCountries(), postcode
                );
                setupCrossBorderData(
                    caseData, eligibilityResult.getLegislativeCountries()
                );
                log.debug(
                    "Cross-border data configured - Country1: {}, Country2: {}",
                    caseData.getCrossBorderCountry1(),
                    caseData.getCrossBorderCountry2()
                );
            }
            case NOT_ELIGIBLE -> {
                log.info("MakeAClaim eligibility check: NOT_ELIGIBLE for postcode {}. "
                        + "Redirecting to PropertyNotEligible page", postcode);
                caseData.setShowPropertyNotEligiblePage(YesOrNo.YES);
            }
            case ELIGIBLE -> {
                log.info("MakeAClaim eligibility check: ELIGIBLE for postcode {}. "
                        + "Proceeding with normal claim flow", postcode);
            }
            case NO_MATCH_FOUND -> {
                log.info("MakeAClaim eligibility check: NO_MATCH_FOUND for postcode {}. "
                        + "Proceeding with default flow", postcode);
            }
            case MULTIPLE_MATCHES_FOUND -> {
                log.info("MakeAClaim eligibility check: MULTIPLE_MATCHES_FOUND for postcode {}. "
                        + "Proceeding with default flow", postcode);
            }
        }

        log.info("MakeAClaim midEvent completed for case ID: {}", details.getId());
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }


    private void validateLegislativeCountries(
        List<LegislativeCountry> legislativeCountries,
        String postcode
    ) {
        if (legislativeCountries == null || legislativeCountries.size() < 2) {
            throw new EligibilityCheckException(
                String.format(
                    "Expected at least 2 legislative countries when status is "
                        + "LEGISLATIVE_COUNTRY_REQUIRED, but got %d for "
                        + "postcode: %s",
                    legislativeCountries == null ? 0
                        : legislativeCountries.size(),
                    postcode
                )
            );
        }
    }

    private void setupCrossBorderData(
        PCSCase caseData,
        List<LegislativeCountry> legislativeCountries
    ) {
        caseData.setShowCrossBorderPage(YesOrNo.YES);

        List<DynamicStringListElement> crossBorderCountries =
            createCrossBorderCountriesList(legislativeCountries);

        DynamicStringList crossBorderCountriesList = DynamicStringList.builder()
            .listItems(crossBorderCountries)
            .build();

        caseData.setCrossBorderCountriesList(crossBorderCountriesList);

        // Set individual cross border countries
        caseData.setCrossBorderCountry1(
            crossBorderCountries.get(0).getLabel()
        );
        caseData.setCrossBorderCountry2(
            crossBorderCountries.get(1).getLabel()
        );
    }

    private List<DynamicStringListElement> createCrossBorderCountriesList(
        List<LegislativeCountry> legislativeCountries
    ) {
        return legislativeCountries.stream()
            .map(value -> DynamicStringListElement.builder()
                .code(value.name())
                .label(value.getLabel())
                .build())
            .toList();
    }
}
