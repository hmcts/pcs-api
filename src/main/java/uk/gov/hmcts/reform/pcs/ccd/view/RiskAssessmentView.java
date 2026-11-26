package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.EnforcementEventShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails.DIFFICULT_ACCESS_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation.ADDITIONAL_INFORMATION_SELECT_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.PeopleToEvict.EVICT_EVERYONE_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails.SELECTED_DEFENDANTS_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails.VULNERABLE_PEOPLE_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails.DEFENDANTS_DOB_KNOWN_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails.RISK_TO_BAILIFF_LABEL;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@Component
@RequiredArgsConstructor
public class RiskAssessmentView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
    private static final Set<EnforcementState> RISK_ASSESSMENT_STATES = EnumSet.of(
        EnforcementState.CASE_PROGRESSION,
        EnforcementState.BREATHING_SPACE,
        EnforcementState.AWAITING_ENFORCEMENT_OUTCOME,
        EnforcementState.AWAITING_PROPERTY_VISIT,
        EnforcementState.AWAITING_SCHEDULING,
        EnforcementState.AWAITING_BAILIFF_ALLOCATION,
        EnforcementState.JUDICIAL_REFERRAL,
        EnforcementState.HEARING_READINESS,
        EnforcementState.PREPARE_FOR_HEARING_CONDUCT_HEARING,
        EnforcementState.CLOSED,
        EnforcementState.CASE_STAYED
    );
    private final RiskAssessmentRenderer renderer;
    private final EnforcementOrderMediator enforcementOrderMediator;
    private final FeatureToggleService featureToggleService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity entity) {
        if (pcsCase.getEnfShowConditions() == null) {
            pcsCase.setEnfShowConditions(EnforcementEventShowConditions.builder().build());
        }
        pcsCase.getEnfShowConditions().setCanViewRiskAssessment(YesOrNo.NO);
        if (!featureToggleService.isEnabled(FeatureFlag.ASSESS_RISK_LEVEL)) {
            return;
        }
        enforcementOrderMediator.getSubmittedWarrantApplication(entity)
            .filter(application -> RISK_ASSESSMENT_STATES.contains(application.getState()))
            .ifPresent(application -> setRiskAssessment(pcsCase, application));
    }

    private void setRiskAssessment(PCSCase pcsCase, EnforcementOrderEntity application) {
        pcsCase.getEnfShowConditions().setCanViewRiskAssessment(YesOrNo.YES);
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
            addAnswer(rows, EVICT_EVERYONE_LABEL, evictEveryone, List.of());
            addSelectedDefendants(rows, raw, evictEveryone);
        }
        addAnswer(rows, DEFENDANTS_DOB_KNOWN_LABEL, warrant.getDefendantsDOBKnown(),
                  warrant.getDefendantsDOBKnown() == VerticalYesNo.YES && warrant.getDefendantsDOB() != null
                      ? details(warrant.getDefendantsDOB().getDefendantsDOBDetails()) : List.of());
        addAnswer(rows, RISK_TO_BAILIFF_LABEL,
                  warrant.getAnyRiskToBailiff(), selectedRisks(warrant));
        addVulnerability(rows, raw);
        if (warrant.getPropertyAccessDetails() != null) {
            var access = warrant.getPropertyAccessDetails();
            addAnswer(rows, DIFFICULT_ACCESS_LABEL, access.getIsDifficultToAccessProperty(),
                      access.getIsDifficultToAccessProperty() == VerticalYesNo.YES
                          ? details(access.getClarificationOnAccessDifficultyText()) : List.of());
        }
        if (warrant.getAdditionalInformation() != null) {
            var additional = warrant.getAdditionalInformation();
            addAnswer(rows, ADDITIONAL_INFORMATION_SELECT_LABEL,
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
            rows.add(new RiskAssessmentRow(SELECTED_DEFENDANTS_LABEL, names.getFirst(),
                                           names.subList(1, names.size())));
        }
    }

    private List<String> selectedRisks(WarrantDetails warrant) {
        if (warrant.getAnyRiskToBailiff() != YesNoNotSure.YES || warrant.getRiskCategories() == null) {
            return List.of();
        }
        List<String> risks = new ArrayList<>();
        Arrays.stream(RiskCategory.values())
            .filter(warrant.getRiskCategories()::contains)
            .forEach(category -> {
                risks.add(category.getLabel());
                risks.addAll(details(riskDescription(category, warrant.getRiskDetails())));
            });
        return risks;
    }

    private String riskDescription(RiskCategory category, RiskDetails riskDetails) {
        if (riskDetails == null) {
            return null;
        }
        return switch (category) {
            case VIOLENT_OR_AGGRESSIVE -> riskDetails.getViolentDetails();
            case FIREARMS_POSSESSION -> riskDetails.getFirearmsDetails();
            case CRIMINAL_OR_ANTISOCIAL -> riskDetails.getCriminalDetails();
            case VERBAL_OR_WRITTEN_THREATS -> riskDetails.getVerbalThreatsDetails();
            case PROTEST_GROUP_MEMBER -> riskDetails.getProtestGroupDetails();
            case AGENCY_VISITS -> riskDetails.getPoliceSocialServicesDetails();
            case AGGRESSIVE_ANIMALS -> riskDetails.getAnimalsDetails();
        };
    }

    private void addVulnerability(List<RiskAssessmentRow> rows, RawWarrantDetails raw) {
        if (raw != null) {
            List<String> vulnerabilityDetails = new ArrayList<>();
            if (raw.getVulnerablePeoplePresent() == YesNoNotSure.YES && raw.getVulnerableAdultsChildren() != null) {
                var vulnerable = raw.getVulnerableAdultsChildren();
                if (vulnerable.getVulnerableCategory() != null) {
                    vulnerabilityDetails.add(vulnerable.getVulnerableCategory().getLabel());
                }
                vulnerabilityDetails.addAll(details(vulnerable.getVulnerableReasonText()));
            }
            addAnswer(rows, VULNERABLE_PEOPLE_LABEL, raw.getVulnerablePeoplePresent(),
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
