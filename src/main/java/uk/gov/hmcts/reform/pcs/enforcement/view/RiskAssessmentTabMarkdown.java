package uk.gov.hmcts.reform.pcs.enforcement.view;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantofrestitution.WarrantOfRestitutionDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.Row;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.escape;
import static uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.heading;
import static uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.paragraph;
import static uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.summaryList;

/**
 * Renders the "Risk assessment" tab (13-risk-assessment-tab.feature, HDPI-7350) in three parts: the print
 * button, the bailiff notes and risk level (internal only), and the claimant's risk answers from their
 * application (internal users and the claimant side).
 */
final class RiskAssessmentTabMarkdown {

    static final String BAILIFF_NOTE = "Bailiff note";
    static final String RISK_ASSESSMENT_LEVEL = "Risk assessment level";
    static final String CLAIMANT_RISK_LEVEL = "Claimant risk level";

    static final String NOTE = "Note";
    static final String ADDED_BY = "Added by";
    static final String DATE_AND_TIME = "Date and time";
    static final String RISK_LEVEL = "Risk level";
    static final String RISK_LEVEL_REASON = "Why the risk level is high";

    static final String NOTE_NOT_ADDED = "Not added yet (click on the 'select action' button above)";
    static final String RISK_LEVEL_NOT_SET = "Not set yet (click on the 'select action' button above)";
    static final String NOT_ANSWERED = "Not answered";
    static final String PRINT = "Print";

    static final String PROVIDED_BY_CLAIMANT_ON =
        "Information provided by the claimant on %s when they were applying for enforcement";
    static final String PROVIDED_BY_CLAIMANT =
        "Information provided by the claimant when they were applying for enforcement";
    static final String PROVIDED_BY_YOU_ON = "Information you provided on %s when you were applying for enforcement";
    static final String PROVIDED_BY_YOU = "Information you provided when you were applying for enforcement";

    static final String EVICT_EVERYONE = "Do you want to evict everyone at the property?";
    static final String WHO_TO_EVICT = "Who do you want to evict?";
    static final String DOB_KNOWN = "Do you know the defendants' dates of birth?";
    static final String DOB_DETAILS = "What are the defendants' dates of birth?";
    static final String ANY_RISK = "Does anyone at the property pose a risk to the bailiff?";
    static final String RISK_KINDS = "What kind of risks do they pose to the bailiff?";
    static final String VULNERABLE = "Is anyone living at the property vulnerable?";
    static final String VULNERABLE_WHO =
        "Confirm if the vulnerable people in the property are adults, children, or both adults and children";
    static final String VULNERABLE_HOW = "How are they vulnerable?";
    static final String DIFFICULT_ACCESS = "Is it difficult to access the property?";
    static final String DIFFICULT_ACCESS_WHY = "Explain why it's difficult to access the property";
    static final String ANYTHING_ELSE = "Do you want to tell us anything else that could help with the eviction?";
    static final String ANYTHING_ELSE_DETAILS = "Tell us anything else that could help with the eviction";

    /**
     * Shown whatever the claimant chose, because the bailiffs always look for them (13-risk-assessment-tab
     * .feature). The other risks are shown only when the claimant selected them.
     */
    private static final Set<RiskCategory> ALWAYS_SHOWN_RISKS = EnumSet.of(
        RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskCategory.CRIMINAL_OR_ANTISOCIAL, RiskCategory.AGGRESSIVE_ANIMALS);

    private static final Map<RiskCategory, Function<RiskDetails, String>> RISK_DETAILS = Map.of(
        RiskCategory.VIOLENT_OR_AGGRESSIVE, RiskDetails::getViolentDetails,
        RiskCategory.FIREARMS_POSSESSION, RiskDetails::getFirearmsDetails,
        RiskCategory.CRIMINAL_OR_ANTISOCIAL, RiskDetails::getCriminalDetails,
        RiskCategory.VERBAL_OR_WRITTEN_THREATS, RiskDetails::getVerbalThreatsDetails,
        RiskCategory.PROTEST_GROUP_MEMBER, RiskDetails::getProtestGroupDetails,
        RiskCategory.AGENCY_VISITS, RiskDetails::getPoliceSocialServicesDetails,
        RiskCategory.AGGRESSIVE_ANIMALS, RiskDetails::getAnimalsDetails
    );

    /**
     * The claimant's risk answers, gathered from either warrant journey.
     */
    record ClaimantAnswers(
        boolean warrantOfPossession,
        HasLabel evictEveryone,
        List<String> peopleToEvict,
        HasLabel datesOfBirthKnown,
        String datesOfBirth,
        HasLabel anyRiskToBailiff,
        Set<RiskCategory> riskCategories,
        RiskDetails riskDetails,
        HasLabel vulnerablePeoplePresent,
        VulnerableAdultsChildren vulnerablePeople,
        PropertyAccessDetails propertyAccess,
        AdditionalInformation additionalInformation
    ) {
    }

    private RiskAssessmentTabMarkdown() {
    }

    /**
     * A GOV.UK button that opens the case on this tab in a new browser tab. XUI sanitises case markdown, so a
     * tab cannot open the print dialogue itself; the new tab is printed with the browser's own print.
     */
    static String printButton(String jurisdiction, String caseType, long caseReference) {
        return ("<a class=\"govuk-button\" href=\"/cases/case-details/%s/%s/%d#Risk%%20assessment\" "
            + "target=\"_blank\" rel=\"noopener noreferrer\">%s</a>")
            .formatted(escape(jurisdiction), escape(caseType), caseReference, PRINT);
    }

    static String internalSections(List<BailiffNoteEntity> notes, RiskLevel riskLevel, String riskLevelReason) {
        List<String> blocks = new ArrayList<>();
        blocks.add(heading(2, BAILIFF_NOTE));
        if (notes.isEmpty()) {
            blocks.add(paragraph(NOTE_NOT_ADDED));
        } else {
            notes.forEach(note -> blocks.add(summaryList(List.of(
                new Row(NOTE, note.getNote()),
                new Row(ADDED_BY, BailiffNoteView.authorName(note)),
                new Row(DATE_AND_TIME, BailiffNoteView.formatAddedOn(note.getCreatedAt()))
            ))));
        }
        blocks.add(heading(2, RISK_ASSESSMENT_LEVEL));
        if (riskLevel == null) {
            blocks.add(paragraph(RISK_LEVEL_NOT_SET));
        } else {
            List<Row> rows = new ArrayList<>();
            rows.add(new Row(RISK_LEVEL, riskLevel.getLabel()));
            if (riskLevel == RiskLevel.HIGH && riskLevelReason != null) {
                rows.add(new Row(RISK_LEVEL_REASON, riskLevelReason));
            }
            blocks.add(summaryList(rows));
        }
        return TabMarkup.lines(blocks);
    }

    /**
     * The claimant's answers, introduced in the third person for internal users and the second person for the
     * claimant side (OPEN-QUESTIONS 15).
     */
    static String claimantSection(ClaimantAnswers answers, Instant submittedAt, boolean forClaimant) {
        String submittedOn = submittedAt == null
            ? null
            : TabMarkup.date(submittedAt.atZone(UK_ZONE_ID).toLocalDate());
        String introduction;
        if (forClaimant) {
            introduction = submittedOn == null ? PROVIDED_BY_YOU : PROVIDED_BY_YOU_ON.formatted(submittedOn);
        } else {
            introduction = submittedOn == null ? PROVIDED_BY_CLAIMANT : PROVIDED_BY_CLAIMANT_ON.formatted(submittedOn);
        }
        return TabMarkup.lines(List.of(
            heading(2, CLAIMANT_RISK_LEVEL),
            paragraph(introduction),
            summaryList(answerRows(answers))
        ));
    }

    private static List<Row> answerRows(ClaimantAnswers answers) {
        List<Row> rows = new ArrayList<>();
        if (answers.warrantOfPossession()) {
            rows.add(new Row(EVICT_EVERYONE, label(answers.evictEveryone())));
            rows.add(new Row(WHO_TO_EVICT, answers.peopleToEvict().isEmpty()
                ? NOT_ANSWERED
                : String.join("\n", answers.peopleToEvict())));
            rows.add(new Row(DOB_KNOWN, label(answers.datesOfBirthKnown())));
            addIfPresent(rows, DOB_DETAILS, answers.datesOfBirth());
        }
        rows.add(new Row(ANY_RISK, label(answers.anyRiskToBailiff())));
        Set<RiskCategory> categories = answers.riskCategories() == null ? Set.of() : answers.riskCategories();
        rows.add(new Row(RISK_KINDS, categories.isEmpty()
            ? NOT_ANSWERED
            : EnumSet.copyOf(categories).stream().map(RiskCategory::getLabel).collect(Collectors.joining("\n"))));
        for (RiskCategory category : RiskCategory.values()) {
            if (ALWAYS_SHOWN_RISKS.contains(category) || categories.contains(category)) {
                String details = Optional.ofNullable(answers.riskDetails())
                    .map(RISK_DETAILS.get(category))
                    .orElse(null);
                rows.add(new Row(category.getText(), orNotAnswered(details)));
            }
        }
        rows.add(new Row(VULNERABLE, label(answers.vulnerablePeoplePresent())));
        Optional.ofNullable(answers.vulnerablePeople()).ifPresent(vulnerable -> {
            if (vulnerable.getVulnerableCategory() != null) {
                rows.add(new Row(VULNERABLE_WHO, vulnerable.getVulnerableCategory().getLabel()));
            }
            addIfPresent(rows, VULNERABLE_HOW, vulnerable.getVulnerableReasonText());
        });
        PropertyAccessDetails access = Optional.ofNullable(answers.propertyAccess())
            .orElseGet(PropertyAccessDetails::new);
        rows.add(new Row(DIFFICULT_ACCESS, label(access.getIsDifficultToAccessProperty())));
        addIfPresent(rows, DIFFICULT_ACCESS_WHY, access.getClarificationOnAccessDifficultyText());
        AdditionalInformation additional = Optional.ofNullable(answers.additionalInformation())
            .orElseGet(AdditionalInformation::new);
        rows.add(new Row(ANYTHING_ELSE, label(additional.getAdditionalInformationSelect())));
        addIfPresent(rows, ANYTHING_ELSE_DETAILS, additional.getAdditionalInformationDetails());
        return rows;
    }

    /**
     * The answers on the claimant's application, for a warrant of possession or a warrant of restitution.
     */
    @SuppressWarnings("deprecation")
    static ClaimantAnswers answersOf(EnforcementOrder order, boolean warrantOfRestitution) {
        if (warrantOfRestitution) {
            WarrantOfRestitutionDetails details = Optional.ofNullable(order.getWarrantOfRestitutionDetails())
                .orElseGet(WarrantOfRestitutionDetails::new);
            RawWarrantRestDetails raw = Optional.ofNullable(order.getRawWarrantRestDetails())
                .orElseGet(RawWarrantRestDetails::new);
            return new ClaimantAnswers(
                false, null, List.of(), null, null,
                details.getAnyRiskToBailiff(),
                details.getRiskCategories(),
                details.getRiskDetails(),
                raw.getVulnerablePeoplePresentWarrantRest(),
                raw.getVulnerableAdultsChildrenWarrantRest(),
                details.getPropertyAccessDetails(),
                details.getAdditionalInformation()
            );
        }
        WarrantDetails details = Optional.ofNullable(order.getWarrantDetails()).orElseGet(WarrantDetails::new);
        RawWarrantDetails raw = Optional.ofNullable(order.getRawWarrantDetails()).orElseGet(RawWarrantDetails::new);
        return new ClaimantAnswers(
            true,
            details.getPeopleToEvict() == null ? null : details.getPeopleToEvict().getEvictEveryone(),
            selectedLabels(raw.getSelectedDefendants()),
            details.getDefendantsDOBKnown(),
            details.getDefendantsDOB() == null ? null : details.getDefendantsDOB().getDefendantsDOBDetails(),
            details.getAnyRiskToBailiff(),
            details.getRiskCategories(),
            details.getRiskDetails(),
            raw.getVulnerablePeoplePresent(),
            raw.getVulnerableAdultsChildren(),
            details.getPropertyAccessDetails(),
            details.getAdditionalInformation()
        );
    }

    private static List<String> selectedLabels(DynamicMultiSelectStringList list) {
        if (list == null || list.getValue() == null) {
            return List.of();
        }
        return list.getValue().stream()
            .filter(Objects::nonNull)
            .map(DynamicStringListElement::getLabel)
            .filter(Objects::nonNull)
            .toList();
    }

    private static void addIfPresent(List<Row> rows, String question, String answer) {
        if (answer != null && !answer.isBlank()) {
            rows.add(new Row(question, answer));
        }
    }

    private static String label(HasLabel answer) {
        return answer == null ? NOT_ANSWERED : answer.getLabel();
    }

    private static String orNotAnswered(String answer) {
        return answer == null || answer.isBlank() ? NOT_ANSWERED : answer;
    }
}
