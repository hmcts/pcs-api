package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.DiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.MandatoryGrounds;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Placeholder page configuration for the Grounds for Possession section.
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
@AllArgsConstructor
@Component
@Slf4j
public class GroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("groundsForPossession", this::midEvent)
                .pageLabel("Grounds for possession")
                .label("groundsForPossession-lineSeparator", "---")
                .mandatory(PCSCase::getGroundsForPossession);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        System.out.println(details.getData().getGroundsForPossession());
        //get the values of enum, then populate the dynamic multi select list and pass that in to the next page.

        DynamicMultiSelectList mandatoryGroundsDynamicList =
            buildDynamicMultiSelectList(MandatoryGrounds.class);

        DynamicMultiSelectList discretionaryGroundsDynamicList =
            buildDynamicMultiSelectList(DiscretionaryGrounds.class);

        PCSCase pcsCase = details.getData();
        pcsCase.setMandatoryGroundsOptionsList(mandatoryGroundsDynamicList);
        pcsCase.setDiscretionaryGroundsOptionsList(discretionaryGroundsDynamicList);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }

    private <E extends Enum<E> & HasLabel> DynamicMultiSelectList buildDynamicMultiSelectList(Class<E> enumClass) {
     //assured tenacy

        List<DynamicListElement> options =
            Arrays.stream(enumClass.getEnumConstants())
                .map(ground -> DynamicListElement.builder()
                    .code(UUID.nameUUIDFromBytes(ground.name().getBytes()))
                    .label(ground.getLabel())
                    .build())
                .toList();

        return DynamicMultiSelectList.builder()
            .listItems(options)
            .build();
    }


}
