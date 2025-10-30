package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantsDOBConcept;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

@AllArgsConstructor
@Component
public class DefendantsDOBConceptPage implements CcdPageConfiguration {

    private final AddressValidator addressValidator;


    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDOBConcept", this::midEvent)
            .pageLabel("The Defendants' dates of birth")
            .label("defendantDOBConcept-lineSeparator", "---")
            .label("defendantDOBConceptLabel", "Do you know the defendant' dates of birth?")
            .complex(PCSCase::getDefendantsDOBConcept)
            .readonly(DefendantsDOBConcept::getFirstName1, "defendantsDOBConcept.firstName1!=\"999\"")
            .optional(DefendantsDOBConcept::getDob1, "defendantsDOBConcept.firstName1!=\"999\"")
            .readonly(DefendantsDOBConcept::getFirstName2, "defendantsDOBConcept.firstName2!=\"999\"")
            .optional(DefendantsDOBConcept::getDob2, "defendantsDOBConcept.firstName2!=\"999\"")
            .readonly(DefendantsDOBConcept::getFirstName3, "defendantsDOBConcept.firstName3!=\"999\"")
            .optional(DefendantsDOBConcept::getDob3, "defendantsDOBConcept.firstName3!=\"999\"")
            .readonly(DefendantsDOBConcept::getFirstName4, "defendantsDOBConcept.firstName4!=\"999\"")
            .optional(DefendantsDOBConcept::getDob4, "defendantsDOBConcept.firstName4!=\"999\"")
            .readonly(DefendantsDOBConcept::getFirstName5, "defendantsDOBConcept.firstName5!=\"999\"")
            .optional(DefendantsDOBConcept::getDob5, "defendantsDOBConcept.firstName5!=\"999\"")
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                    CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
    }
}
