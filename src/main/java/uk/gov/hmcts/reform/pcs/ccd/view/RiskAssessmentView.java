package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.riskassessment.RiskAssessmentRow;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.RiskAssessmentRenderer;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@Component
@RequiredArgsConstructor
public class RiskAssessmentView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
    private final RiskAssessmentRenderer renderer;
    private final EnforcementOrderMediator enforcementOrderMediator;
    private final FeatureToggleService featureToggleService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity entity) {
        pcsCase.setRiskAssessmentInternalMarkdown(null);
        pcsCase.setRiskAssessmentClaimantMarkdown(null);
        pcsCase.setRiskAssessmentInternalMarkdownLabel(null);
        pcsCase.setRiskAssessmentClaimantMarkdownLabel(null);
        if (!featureToggleService.isEnabled(FeatureFlag.ASSESS_RISK_LEVEL)) {
            return;
        }
        enforcementOrderMediator.getSubmittedWarrantApplication(entity)
            .ifPresent(application -> setRiskAssessment(pcsCase, application));
    }

    private void setRiskAssessment(PCSCase pcsCase, EnforcementOrderEntity application) {
        String submittedDate = application.getStatementOfTruth().getCompletedDate()
            .atZone(ZoneOffset.UTC).withZoneSameInstant(UK_ZONE_ID).toLocalDate().format(DATE_FORMAT);
        List<RiskAssessmentRow> rows = buildRows(application.getEnforcementOrder());
        pcsCase.setRiskAssessmentInternalMarkdown(renderer.render(submittedDate, rows, false));
        pcsCase.setRiskAssessmentClaimantMarkdown(renderer.render(submittedDate, rows, true));
    }

    private List<RiskAssessmentRow> buildRows(EnforcementOrder order) {
        List<RiskAssessmentRow> rows = new ArrayList<>();
        WarrantDetails warrant = order.getWarrantDetails();
        RawWarrantDetails raw = order.getRawWarrantDetails();
        if (warrant.getPeopleToEvict() != null) {
            VerticalYesNo evictEveryone = warrant.getPeopleToEvict().getEvictEveryone();
            addAnswer(rows, "Do you want to evict everyone at the property?", evictEveryone, List.of());
            addSelectedDefendants(rows, raw, evictEveryone);
        }
        addAnswer(rows, "Do you know the defendants' date of birth?", warrant.getDefendantsDOBKnown(), List.of());
        addAnswer(rows, "Does anyone living at the property pose a risk to the bailiff?",
                  warrant.getAnyRiskToBailiff(), selectedRisks(warrant));
        addVulnerability(rows, raw);
        if (warrant.getPropertyAccessDetails() != null) {
            var access = warrant.getPropertyAccessDetails();
            addAnswer(rows, "Is it difficult to access the property?", access.getIsDifficultToAccessProperty(),
                      access.getIsDifficultToAccessProperty() == VerticalYesNo.YES
                          ? details(access.getClarificationOnAccessDifficultyText()) : List.of());
        }
        if (warrant.getAdditionalInformation() != null) {
            var additional = warrant.getAdditionalInformation();
            addAnswer(rows, "Do you want to tell us anything else that could help with the eviction?",
                      additional.getAdditionalInformationSelect(),
                      additional.getAdditionalInformationSelect() == VerticalYesNo.YES
                          ? details(additional.getAdditionalInformationDetails()) : List.of());
        }
        return rows;
    }

    private void addSelectedDefendants(List<RiskAssessmentRow> rows, RawWarrantDetails raw,
                                       VerticalYesNo evictEveryone) {
        if (evictEveryone != VerticalYesNo.NO || raw == null || raw.getSelectedDefendants() == null
            || raw.getSelectedDefendants().getValue() == null) {
            return;
        }
        List<String> names = raw.getSelectedDefendants().getValue().stream()
            .map(DynamicStringListElement::getLabel)
            .filter(name -> name != null && !name.isBlank())
            .toList();
        if (!names.isEmpty()) {
            rows.add(new RiskAssessmentRow("Who do you want to evict?", names.getFirst(),
                                           names.subList(1, names.size())));
        }
    }

    private List<String> selectedRisks(WarrantDetails warrant) {
        if (warrant.getAnyRiskToBailiff() != YesNoNotSure.YES || warrant.getRiskCategories() == null) {
            return List.of();
        }
        return Arrays.stream(RiskCategory.values())
            .filter(warrant.getRiskCategories()::contains)
            .map(RiskCategory::getLabel)
            .toList();
    }

    private void addVulnerability(List<RiskAssessmentRow> rows, RawWarrantDetails raw) {
        if (raw != null) {
            List<String> vulnerabilityDetails = raw.getVulnerablePeoplePresent() == YesNoNotSure.YES
                && raw.getVulnerableAdultsChildren() != null
                ? details(raw.getVulnerableAdultsChildren().getVulnerableReasonText()) : List.of();
            addAnswer(rows, "Is anyone living at the property vulnerable?", raw.getVulnerablePeoplePresent(),
                      vulnerabilityDetails);
        }
    }

    private void addAnswer(List<RiskAssessmentRow> rows, String question, HasLabel answer, List<String> details) {
        if (answer != null) {
            rows.add(new RiskAssessmentRow(question, answer.getLabel(), details));
        }
    }

    private List<String> details(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }
}
