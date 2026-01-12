package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class DefendantCircumstancesPage implements CcdPageConfiguration {

    private final TextValidationService textValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantCircumstances", this::midEvent)
            .pageLabel("Defendants’ circumstances")
            .complex(PCSCase::getDefendantCircumstances)
            .mandatory(DefendantCircumstances::getDefendantTermPossessive,NEVER_SHOW)
            .readonlyNoSummary(DefendantCircumstances::getDefendantCircumstancesLabel)
            .mandatory(DefendantCircumstances::getHasDefendantCircumstancesInfo)
            .mandatory(DefendantCircumstances::getDefendantCircumstancesInfo,
                       "hasDefendantCircumstancesInfo=\"YES\"")
            .done()
            .label("defendantCircumstances-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        DefendantCircumstances defendantCircumstances = caseData.getDefendantCircumstances();
        if (defendantCircumstances != null) {
            // Use fallback if defendantTermPossessive is not set
            String defendantTerm = defendantCircumstances.getDefendantTermPossessive();
            if (defendantTerm == null || defendantTerm.trim().isEmpty()) {
                defendantTerm = "defendants’";
            }

            String dynamicLabel = "Give details about the " + defendantTerm + " circumstances";

            validationErrors.addAll(textValidationService.validateSingleTextArea(
                defendantCircumstances.getDefendantCircumstancesInfo(),
                dynamicLabel,
                TextValidationService.LONG_TEXT_LIMIT
            ));
        }

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }
}
