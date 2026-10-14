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
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class DefendantCircumstancesPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    private static final String SINGLE_DEFENDANT = "addAnotherDefendant=\"NO\"";
    private static final String MULTIPLE_DEFENDANTS = "addAnotherDefendant=\"YES\"";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantCircumstances", this::midEvent)
            .pageLabel("Defendants’ circumstances")
            .complex(PCSCase::getDefendantCircumstances)
            .readonly(DefendantCircumstances::getDefendantTermPossessive, NEVER_SHOW, true)
            .readonlyNoSummary(DefendantCircumstances::getDefendantCircumstancesLabel, SINGLE_DEFENDANT)
            .readonlyNoSummary(DefendantCircumstances::getDefendantsCircumstancesLabel, MULTIPLE_DEFENDANTS)
            .mandatory(DefendantCircumstances::getHasSingleDefendantCircumstancesInfo, SINGLE_DEFENDANT)
            .mandatory(DefendantCircumstances::getSingleDefendantCircumstancesInfo,
                       SINGLE_DEFENDANT + " AND hasSingleDefendantCircumstancesInfo=\"YES\"")
            .mandatory(DefendantCircumstances::getHasMultipleDefendantsCircumstancesInfo, MULTIPLE_DEFENDANTS)
            .mandatory(DefendantCircumstances::getMultipleDefendantsCircumstancesInfo,
                       MULTIPLE_DEFENDANTS + " AND hasMultipleDefendantsCircumstancesInfo=\"YES\"")
            .readonly(DefendantCircumstances::getHasDefendantCircumstancesInfo, NEVER_SHOW, true)
            .readonly(DefendantCircumstances::getDefendantCircumstancesInfo, NEVER_SHOW, true)
            .done()
            .label("defendantCircumstances-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        DefendantCircumstances defendantCircumstances = caseData.getDefendantCircumstances();
        if (defendantCircumstances != null) {
            boolean multipleDefendants = caseData.getAddAnotherDefendant() == VerticalYesNo.YES;
            saveAnswersFromShownFields(defendantCircumstances, multipleDefendants);

            String detailsLabel = multipleDefendants
                ? "Give details about the defendants’ circumstances"
                : "Give details about the defendant’s circumstances";

            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                defendantCircumstances.getDefendantCircumstancesInfo(),
                detailsLabel,
                TextAreaValidationService.LONG_TEXT_LIMIT
            ));
        }

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private static void saveAnswersFromShownFields(DefendantCircumstances circumstances, boolean multipleDefendants) {
        if (multipleDefendants) {
            circumstances.setHasDefendantCircumstancesInfo(circumstances.getHasMultipleDefendantsCircumstancesInfo());
            circumstances.setDefendantCircumstancesInfo(circumstances.getMultipleDefendantsCircumstancesInfo());
        } else {
            circumstances.setHasDefendantCircumstancesInfo(circumstances.getHasSingleDefendantCircumstancesInfo());
            circumstances.setDefendantCircumstancesInfo(circumstances.getSingleDefendantCircumstancesInfo());
        }
    }
}
