package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * Proof of concept page for a Welsh translation feature request. Not for merge.
 *
 * <p>ExUI fills in {@code ${...}} placeholders in a field's label and hint text. For a Label field it translates
 * the template first and then fills in the values, so one dictionary row covers every value. It does not do this
 * for the options of a dynamic list: each option is rendered as {@code radioButton.label | rpxTranslate}.
 *
 * <p>This page uses the same template twice: as a Label field, where the placeholders are filled in, and as the
 * first option of a DynamicRadioList, where they are shown as typed. The second option is today's approach: a
 * sentence the service has already filled in, which only a dictionary row for that exact sentence can translate.
 *
 * <p>Every phrase starts with {@code TEST-}, because that is the only prefix ts-translation-service can delete.
 */
public class WelshDynamicListInterpolationPoc implements CcdPageConfiguration {

    public static final String TEMPLATE =
        "TEST- Yes, the documents relate to General app (GA${welshPocGaRank}) - Claimant ${welshPocPartyRank}"
            + " - submitted on ${welshPocSubmittedDate}";

    public static final String RESOLVED =
        "TEST- Yes, the documents relate to General app (GA3) - Claimant 1 - submitted on Monday 16 November 2026";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("welshDynamicListInterpolationPoc")
            .pageLabel("TEST- Welsh dynamic list interpolation proof of concept")
            .readonly(PCSCase::getWelshPocGaRank, NEVER_SHOW, true)
            .readonly(PCSCase::getWelshPocPartyRank, NEVER_SHOW, true)
            .readonly(PCSCase::getWelshPocSubmittedDate, NEVER_SHOW, true)
            .label("welshPocTemplateAsLabel", TEMPLATE)
            .optional(PCSCase::getWelshPocDynamicRadioList);
    }

    /**
     * Sets the values the placeholders point at, and the two options. Called from the event's start handler,
     * which is where LegalRepDocumentUpload builds its real options.
     */
    public static void populate(PCSCase caseData) {
        caseData.setWelshPocGaRank("3");
        caseData.setWelshPocPartyRank("1");
        caseData.setWelshPocSubmittedDate(LocalDate.of(2026, 11, 16));
        caseData.setWelshPocDynamicRadioList(
            DynamicStringList.builder()
                .listItems(List.of(
                    DynamicStringListElement.builder().code("TEMPLATE").label(TEMPLATE).build(),
                    DynamicStringListElement.builder().code("RESOLVED").label(RESOLVED).build()
                ))
                .build()
        );
    }
}
