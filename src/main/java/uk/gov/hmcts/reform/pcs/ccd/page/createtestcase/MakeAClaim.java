package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

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
            caseData.setShowCrossBorderPage(YesOrNo.YES);
            List<DynamicStringListElement> crossBorderCountries = eligibilityResult.getLegislativeCountries().stream()
                .map(value -> DynamicStringListElement.builder().code(value.name()).label(value.getLabel()).build())
                .toList();

            DynamicStringList crossBorderCountriesList = DynamicStringList.builder()
                .listItems(crossBorderCountries)
                .build();
            caseData.setCrossBorderCountriesList(crossBorderCountriesList);

            // Set individual cross border country fields
            if (crossBorderCountries.size() > 0) {
                caseData.setCrossBorderCountry1(crossBorderCountries.get(0).getLabel());
            }
            if (crossBorderCountries.size() > 1) {
                caseData.setCrossBorderCountry2(crossBorderCountries.get(1).getLabel());
            }
        } else {
            caseData.setShowCrossBorderPage(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
