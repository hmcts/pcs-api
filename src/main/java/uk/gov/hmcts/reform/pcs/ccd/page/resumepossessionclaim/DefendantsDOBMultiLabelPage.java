package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantsDOBMultiLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

@AllArgsConstructor
@Component
public class DefendantsDOBMultiLabelPage implements CcdPageConfiguration {

    private final AddressValidator addressValidator;


    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDOBConcept", this::midEvent)
            .pageLabel("The Defendants' dates of birth")
            .label("defendantDOBConceptLabel", """
                ---
                <h2>Do you know the defendant' dates of birth?</h2>""")
            .complex(PCSCase::getDefendantsDOBMultiLabel)
            .readonly(DefendantsDOBMultiLabel::getFirstName1, "defendantsDOBMultiLabel.firstName1!=\"999\"")
            .optional(DefendantsDOBMultiLabel::getDob1, "defendantsDOBMultiLabel.firstName1!=\"999\"")
            .readonly(DefendantsDOBMultiLabel::getFirstName2, "defendantsDOBMultiLabel.firstName2!=\"999\"")
            .optional(DefendantsDOBMultiLabel::getDob2, "defendantsDOBMultiLabel.firstName2!=\"999\"")
            .readonly(DefendantsDOBMultiLabel::getFirstName3, "defendantsDOBMultiLabel.firstName3!=\"999\"")
            .optional(DefendantsDOBMultiLabel::getDob3, "defendantsDOBMultiLabel.firstName3!=\"999\"")
            .readonly(DefendantsDOBMultiLabel::getFirstName4, "defendantsDOBMultiLabel.firstName4!=\"999\"")
            .optional(DefendantsDOBMultiLabel::getDob4, "defendantsDOBMultiLabel.firstName4!=\"999\"")
            .readonly(DefendantsDOBMultiLabel::getFirstName5, "defendantsDOBMultiLabel.firstName5!=\"999\"")
            .optional(DefendantsDOBMultiLabel::getDob5, "defendantsDOBMultiLabel.firstName5!=\"999\"")
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                    CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
    }
}
