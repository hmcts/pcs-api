package uk.gov.hmcts.reform.pcs.enforcement.view;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;
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
 * application, with any they changed while confirming the eviction (internal users and the claimant side).
 * While a change waits for the bailiff's review, the internal part starts with a banner listing what changed.
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
    static final String PRINT = "Print (opens in a new tab)";

    static final String PROVIDED_BY_CLAIMANT_ON =
        "Information provided by the claimant on %s when they were applying for enforcement";
    static final String PROVIDED_BY_CLAIMANT =
        "Information provided by the claimant when they were applying for enforcement";
    static final String PROVIDED_BY_YOU_ON = "Information you provided on %s when you were applying for enforcement";
    static final String PROVIDED_BY_YOU = "Information you provided when you were applying for enforcement";
    static final String UPDATED_BY_CLAIMANT_ON = "The claimant updated this information on %s when they confirmed the "
        + "eviction";
    static final String UPDATED_BY_YOU_ON = "You updated this information on %s when you confirmed the eviction";
    static final String UPDATE_BANNER = "The claimant has updated their risk assessment.";
    static final String UPDATE_BANNER_CHANGED = "Review the changed answers and set the risk level. Changed:";

    static final String QUESTION = "Question";
    static final String ORIGINAL_ANSWER = "Original answer";
    static final String UPDATED_ANSWER = "Updated answer";

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
     * claimant side (OPEN-QUESTIONS 38).
     */
    static String claimantSection(ClaimantAnswers answers, Instant submittedAt, boolean forClaimant) {
        return claimantSection(answers, submittedAt, null, null, forClaimant);
    }

    /**
     * The claimant's answers, as above. Once they have changed any in "Confirm the eviction details"
     * (HDPI-7552), every question is shown with its original answer and, where it changed, the updated one.
     *
     * @param updated the claimant's current answers, or null if they have never changed them
     * @param updatedAt when they last changed them
     */
    static String claimantSection(ClaimantAnswers original, Instant submittedAt, ClaimantAnswers updated,
                                  Instant updatedAt, boolean forClaimant) {
        String submittedOn = ukDate(submittedAt);
        String introduction;
        if (forClaimant) {
            introduction = submittedOn == null ? PROVIDED_BY_YOU : PROVIDED_BY_YOU_ON.formatted(submittedOn);
        } else {
            introduction = submittedOn == null ? PROVIDED_BY_CLAIMANT : PROVIDED_BY_CLAIMANT_ON.formatted(submittedOn);
        }
        if (updated == null) {
            return TabMarkup.lines(List.of(
                heading(2, CLAIMANT_RISK_LEVEL),
                paragraph(introduction),
                summaryList(answerRows(original))
            ));
        }
        String updatedOn = ukDate(updatedAt);
        String update = forClaimant ? UPDATED_BY_YOU_ON : UPDATED_BY_CLAIMANT_ON;
        List<List<String>> rows = comparedAnswers(original, updated).stream()
            .map(answer -> List.of(answer.question(), answer.original(), answer.changed() ? answer.updated() : ""))
            .toList();
        return TabMarkup.lines(List.of(
            heading(2, CLAIMANT_RISK_LEVEL),
            paragraph(introduction),
            paragraph(update.formatted(updatedOn)),
            TabMarkup.table(List.of(QUESTION, ORIGINAL_ANSWER, UPDATED_ANSWER), rows)
        ));
    }

    /**
     * Tells bailiff staff that the claimant has changed their answers since the risk level was last set, and
     * which questions changed. Internal only.
     */
    static String updateBanner(ClaimantAnswers original, ClaimantAnswers updated) {
        String changed = comparedAnswers(original, updated).stream()
            .filter(ComparedAnswer::changed)
            .map(answer -> "<li>" + escape(answer.question()) + "</li>")
            .collect(Collectors.joining("\n", "<ul class=\"govuk-list govuk-list--bullet\">\n", "\n</ul>"));
        return TabMarkup.lines(List.of(
            "<div class=\"govuk-notification-banner\" role=\"region\" aria-labelledby=\"claimant-risk-update\">",
            "<div class=\"govuk-notification-banner__header\">"
                + "<h2 class=\"govuk-notification-banner__title\" id=\"claimant-risk-update\">Important</h2></div>",
            "<div class=\"govuk-notification-banner__content\">",
            "<p class=\"govuk-notification-banner__heading\">" + escape(UPDATE_BANNER) + "</p>",
            paragraph(UPDATE_BANNER_CHANGED),
            changed,
            "</div>",
            "</div>"
        ));
    }

    /**
     * The claimant's application answers with their risk answers replaced, so that answers changed in "Confirm
     * the eviction details" are shown and compared the same way as the application's.
     */
    static ClaimantAnswers withRiskAnswers(ClaimantAnswers application, ClaimantRiskAnswers risk) {
        return new ClaimantAnswers(
            application.warrantOfPossession(),
            application.evictEveryone(),
            application.peopleToEvict(),
            application.datesOfBirthKnown(),
            application.datesOfBirth(),
            risk.getAnyRiskToBailiff(),
            risk.getRiskCategories(),
            RiskDetails.builder()
                .violentDetails(risk.getViolentDetails())
                .firearmsDetails(risk.getFirearmsDetails())
                .criminalDetails(risk.getCriminalDetails())
                .verbalThreatsDetails(risk.getVerbalThreatsDetails())
                .protestGroupDetails(risk.getProtestGroupDetails())
                .policeSocialServicesDetails(risk.getPoliceSocialServicesDetails())
                .animalsDetails(risk.getAnimalsDetails())
                .build(),
            risk.getVulnerablePeoplePresent(),
            new VulnerableAdultsChildren(risk.getVulnerableCategory(), risk.getVulnerableReasonText()),
            new PropertyAccessDetails(risk.getIsDifficultToAccessProperty(),
                                      risk.getClarificationOnAccessDifficultyText()),
            new AdditionalInformation(risk.getAdditionalInformationSelect(), risk.getAdditionalInformationDetails())
        );
    }

    /**
     * Every question either set of answers shows, in order, with both answers.
     */
    static List<ComparedAnswer> comparedAnswers(ClaimantAnswers original, ClaimantAnswers updated) {
        List<ComparedAnswer> compared = new ArrayList<>();
        for (Question question : QUESTIONS) {
            Optional<String> before = question.answer().apply(original);
            Optional<String> after = question.answer().apply(updated);
            if (before.isPresent() || after.isPresent()) {
                compared.add(new ComparedAnswer(question.key(), before.orElse(NOT_ANSWERED),
                                                after.orElse(NOT_ANSWERED)));
            }
        }
        return compared;
    }

    record ComparedAnswer(String question, String original, String updated) {
        boolean changed() {
            return !original.equals(updated);
        }
    }

    /**
     * A row of the claimant's answers: the question, and the answer to show, or empty when the row is left out
     * for these answers.
     */
    private record Question(String key, Function<ClaimantAnswers, Optional<String>> answer) {
    }

    private static final List<Question> QUESTIONS = questions();

    private static List<Question> questions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question(EVICT_EVERYONE, a -> possessionOnly(a, label(a.evictEveryone()))));
        questions.add(new Question(WHO_TO_EVICT, a -> possessionOnly(a, a.peopleToEvict().isEmpty()
            ? NOT_ANSWERED
            : String.join("\n", a.peopleToEvict()))));
        questions.add(new Question(DOB_KNOWN, a -> possessionOnly(a, label(a.datesOfBirthKnown()))));
        questions.add(new Question(DOB_DETAILS, a -> a.warrantOfPossession() ? present(a.datesOfBirth())
            : Optional.empty()));
        questions.add(new Question(ANY_RISK, a -> Optional.of(label(a.anyRiskToBailiff()))));
        questions.add(new Question(RISK_KINDS, a -> {
            Set<RiskCategory> categories = categoriesOf(a);
            return Optional.of(categories.isEmpty()
                ? NOT_ANSWERED
                : EnumSet.copyOf(categories).stream().map(RiskCategory::getLabel)
                    .collect(Collectors.joining("\n")));
        }));
        for (RiskCategory category : RiskCategory.values()) {
            questions.add(new Question(category.getText(), a -> {
                if (!ALWAYS_SHOWN_RISKS.contains(category) && !categoriesOf(a).contains(category)) {
                    return Optional.empty();
                }
                return Optional.of(orNotAnswered(Optional.ofNullable(a.riskDetails())
                    .map(RISK_DETAILS.get(category))
                    .orElse(null)));
            }));
        }
        questions.add(new Question(VULNERABLE, a -> Optional.of(label(a.vulnerablePeoplePresent()))));
        questions.add(new Question(VULNERABLE_WHO, a -> Optional.ofNullable(a.vulnerablePeople())
            .map(VulnerableAdultsChildren::getVulnerableCategory)
            .map(HasLabel::getLabel)));
        questions.add(new Question(VULNERABLE_HOW, a -> Optional.ofNullable(a.vulnerablePeople())
            .flatMap(vulnerable -> present(vulnerable.getVulnerableReasonText()))));
        questions.add(new Question(DIFFICULT_ACCESS, a -> Optional.of(label(accessOf(a)
            .getIsDifficultToAccessProperty()))));
        questions.add(new Question(DIFFICULT_ACCESS_WHY, a -> present(accessOf(a)
            .getClarificationOnAccessDifficultyText())));
        questions.add(new Question(ANYTHING_ELSE, a -> Optional.of(label(additionalOf(a)
            .getAdditionalInformationSelect()))));
        questions.add(new Question(ANYTHING_ELSE_DETAILS, a -> present(additionalOf(a)
            .getAdditionalInformationDetails())));
        return List.copyOf(questions);
    }

    private static List<Row> answerRows(ClaimantAnswers answers) {
        List<Row> rows = new ArrayList<>();
        for (Question question : QUESTIONS) {
            question.answer().apply(answers).ifPresent(answer -> rows.add(new Row(question.key(), answer)));
        }
        return rows;
    }

    private static Optional<String> possessionOnly(ClaimantAnswers answers, String answer) {
        return answers.warrantOfPossession() ? Optional.of(answer) : Optional.empty();
    }

    private static Optional<String> present(String answer) {
        return answer == null || answer.isBlank() ? Optional.empty() : Optional.of(answer);
    }

    private static Set<RiskCategory> categoriesOf(ClaimantAnswers answers) {
        return answers.riskCategories() == null ? Set.of() : answers.riskCategories();
    }

    private static PropertyAccessDetails accessOf(ClaimantAnswers answers) {
        return Optional.ofNullable(answers.propertyAccess()).orElseGet(PropertyAccessDetails::new);
    }

    private static AdditionalInformation additionalOf(ClaimantAnswers answers) {
        return Optional.ofNullable(answers.additionalInformation()).orElseGet(AdditionalInformation::new);
    }

    private static String ukDate(Instant instant) {
        return instant == null ? null : TabMarkup.formatDate(instant.atZone(UK_ZONE_ID).toLocalDate());
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

    private static String label(HasLabel answer) {
        return answer == null ? NOT_ANSWERED : answer.getLabel();
    }

    private static String orNotAnswered(String answer) {
        return answer == null || answer.isBlank() ? NOT_ANSWERED : answer;
    }
}
