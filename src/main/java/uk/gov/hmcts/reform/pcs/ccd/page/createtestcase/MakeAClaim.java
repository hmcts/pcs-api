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
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
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
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        String postcode = caseData.getPropertyAddress().getPostCode();
        EligibilityResult eligibilityResult = eligibilityService.checkEligibility(postcode, null);
        if (eligibilityResult.getStatus() == EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED) {
            validateLegislativeCountries(eligibilityResult.getLegislativeCountries(), postcode);
            setupCrossBorderData(caseData, eligibilityResult.getLegislativeCountries());
        } else {
            caseData.setShowCrossBorderPage(YesOrNo.NO);
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
