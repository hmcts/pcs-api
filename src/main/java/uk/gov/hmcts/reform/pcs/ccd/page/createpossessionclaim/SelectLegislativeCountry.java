package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.type.poc.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.type.poc.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Arrays;
import java.util.List;

public class SelectLegislativeCountry implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectLegislativeCountry", this::midEvent)
            .pageLabel("Legislative Country (placeholder)")
            .label("selectLegislativeCountry-info", """
                ---
                Temporary page - this will be replaced with a check on the property postcode.

                Select only England or Wales to proceed.
                """)
            .mandatory(PCSCase::getLegislativeCountryChoice);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();
        LegislativeCountry propertyLegislativeCountry = caseData.getLegislativeCountryChoice();

        caseData.setLegislativeCountry(propertyLegislativeCountry.getLabel());
        List<DynamicStringListElement> listItems = Arrays.stream(ClaimantType.values())
            .filter(value -> value.isApplicableFor(propertyLegislativeCountry))
            .map(value -> DynamicStringListElement.builder().code(value.name()).label(value.getLabel()).build())
            .toList();

        DynamicList claimantTypeList = DynamicList.builder()
            .listItems(listItems)
            .build();
        caseData.setClaimantType(claimantTypeList);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
