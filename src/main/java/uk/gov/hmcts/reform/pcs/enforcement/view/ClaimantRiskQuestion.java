package uk.gov.hmcts.reform.pcs.enforcement.view;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.ClaimantRiskAnswers;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.RiskCategory;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.AdditionalInformation.ADDITIONAL_INFORMATION_DETAILS_LABEL;

/**
 * The risk assessment questions, in the order the warrant application asks them, with the text of the
 * claimant's answer to each.
 */
enum ClaimantRiskQuestion {

    ANY_RISK_TO_BAILIFF("Does anyone living at the property pose a risk to the bailiff?",
                        answers -> label(answers.getAnyRiskToBailiff())),
    RISK_CATEGORIES("What kind of risks do they pose to the bailiff?",
                    answers -> categories(answers.getRiskCategories())),
    VIOLENT_DETAILS(RiskCategory.VIOLENT_OR_AGGRESSIVE.getText(), ClaimantRiskAnswers::getViolentDetails),
    FIREARMS_DETAILS(RiskCategory.FIREARMS_POSSESSION.getText(), ClaimantRiskAnswers::getFirearmsDetails),
    CRIMINAL_DETAILS(RiskCategory.CRIMINAL_OR_ANTISOCIAL.getText(), ClaimantRiskAnswers::getCriminalDetails),
    VERBAL_THREATS_DETAILS(RiskCategory.VERBAL_OR_WRITTEN_THREATS.getText(),
                           ClaimantRiskAnswers::getVerbalThreatsDetails),
    PROTEST_GROUP_DETAILS(RiskCategory.PROTEST_GROUP_MEMBER.getText(), ClaimantRiskAnswers::getProtestGroupDetails),
    POLICE_SOCIAL_SERVICES_DETAILS(RiskCategory.AGENCY_VISITS.getText(),
                                   ClaimantRiskAnswers::getPoliceSocialServicesDetails),
    ANIMALS_DETAILS(RiskCategory.AGGRESSIVE_ANIMALS.getText(), ClaimantRiskAnswers::getAnimalsDetails),
    VULNERABLE_PEOPLE_PRESENT("Is anyone living at the property vulnerable?",
                              answers -> label(answers.getVulnerablePeoplePresent())),
    VULNERABLE_CATEGORY("Confirm if the vulnerable people in the property are adults, children, or both adults and "
                            + "children", answers -> label(answers.getVulnerableCategory())),
    VULNERABLE_REASON("How are they vulnerable?", ClaimantRiskAnswers::getVulnerableReasonText),
    DIFFICULT_TO_ACCESS("Is it difficult to access the property?",
                        answers -> label(answers.getIsDifficultToAccessProperty())),
    ACCESS_DIFFICULTY_DETAILS("Explain why it’s difficult to access the property",
                              ClaimantRiskAnswers::getClarificationOnAccessDifficultyText),
    ADDITIONAL_INFORMATION("Do you want to tell us anything else that could help with the eviction?",
                           answers -> label(answers.getAdditionalInformationSelect())),
    ADDITIONAL_INFORMATION_DETAILS(ADDITIONAL_INFORMATION_DETAILS_LABEL,
                                   ClaimantRiskAnswers::getAdditionalInformationDetails);

    private final String question;
    private final Function<ClaimantRiskAnswers, String> answer;

    ClaimantRiskQuestion(String question, Function<ClaimantRiskAnswers, String> answer) {
        this.question = question;
        this.answer = answer;
    }

    String getQuestion() {
        return question;
    }

    /**
     * The answer's text, or null if there is no answer.
     */
    String answerIn(ClaimantRiskAnswers answers) {
        if (answers == null) {
            return null;
        }
        String text = answer.apply(answers);
        return text == null || text.isBlank() ? null : text;
    }

    private static String label(HasLabel value) {
        return value == null ? null : value.getLabel();
    }

    private static String categories(Set<RiskCategory> categories) {
        if (categories == null) {
            return null;
        }
        return categories.stream()
            .sorted(Comparator.naturalOrder())
            .map(RiskCategory::getLabel)
            .collect(Collectors.joining("\n"));
    }
}
