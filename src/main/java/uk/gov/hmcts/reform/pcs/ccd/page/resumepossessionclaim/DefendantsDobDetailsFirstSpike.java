package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDOBDynamicDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@Component
public class DefendantsDobDetailsFirstSpike implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {

        pageBuilder
            .page("defendantsDobDetails", this::midEvent)
            .label("defDobLine", "---")
            .pageLabel("The defendants’ dates of birth (original spike)")
            .mandatory(PCSCase::getDefendantsDynamic)
            .mandatory(PCSCase::getDobKnown)
            .optional(PCSCase::getDefendantsDOB, "dobKnown=\"YES\"");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();


        Set<ListValue<DefendantDOBDynamicDetails>> defendantKnownDOB = new LinkedHashSet<>();
        System.out.println(caseData.getDefendantsDynamic().getValue());
        int index = 0;
        for (DynamicListElement element : caseData.getDefendantsDynamic().getValue()) {
            // Build the DOB detail with name and empty dob
            DefendantDOBDynamicDetails defDy = DefendantDOBDynamicDetails.builder()
                .name(element.getLabel())   // from DynamicMultiSelectList item
                .dob(null)                  // blank for now
                .build();

            // Wrap it in a ListValue (required by CCD)
            ListValue<DefendantDOBDynamicDetails> listValue =
                ListValue.<DefendantDOBDynamicDetails>builder()
                    .id(String.valueOf(index++))
                    .value(defDy)
                    .build();

            defendantKnownDOB.add(listValue);
        }

        caseData.setDefendantKnownDOB(defendantKnownDOB);
        System.out.println(caseData.getDefendantKnownDOB());

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }



}
