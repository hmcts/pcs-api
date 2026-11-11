package uk.gov.hmcts.reform.pcs.hearings.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndividualDetailsFlagMapperTest {

    private final IndividualDetailsFlagMapper underTest = new IndividualDetailsFlagMapper();

    @Test
    void shouldReturnEmptyDetailsWhenFlagsAreNull() {
        IndividualDetails result = underTest.map(null);

        assertThat(result.getVulnerableFlag()).isFalse();
        assertThat(result.getVulnerabilityDetails()).isNull();
        assertThat(result.getInterpreterLanguage()).isNull();
        assertThat(result.getReasonableAdjustments()).isNull();
        assertThat(result.getOtherReasonableAdjustmentDetails()).isNull();
    }

    @Test
    void shouldIgnoreInactiveFlags() {
        IndividualDetails result = underTest.map(List.of(
            flag("PF0002", "Vulnerable user", "Inactive", "comment", false),
            flag("RA0019", "Step free / wheelchair access", "Inactive", null, true),
            flag("SM0003", "Evidence by live link", "Inactive", "comment", true)
        ));

        assertThat(result.getVulnerableFlag()).isFalse();
        assertThat(result.getReasonableAdjustments()).isNull();
        assertThat(result.getOtherReasonableAdjustmentDetails()).isNull();
    }

    @Nested
    class VulnerableUser {

        @Test
        void shouldSetVulnerableFlagAndDetailsWhenActiveWithComment() {
            IndividualDetails result = underTest.map(List.of(
                flag("PF0002", "Vulnerable user", "Active", "Anxious in court", false)
            ));

            assertThat(result.getVulnerableFlag()).isTrue();
            assertThat(result.getVulnerabilityDetails()).isEqualTo("Anxious in court");
        }

        @Test
        void shouldOmitVulnerabilityDetailsWhenNoComment() {
            IndividualDetails result = underTest.map(List.of(
                flag("PF0002", "Vulnerable user", "Active", null, false)
            ));

            assertThat(result.getVulnerableFlag()).isTrue();
            assertThat(result.getVulnerabilityDetails()).isNull();
        }

        @Test
        void shouldNeverIncludeVulnerableUserInOtherDetails() {
            IndividualDetails result = underTest.map(List.of(
                flag("PF0002", "Vulnerable user", "Active", "Anxious in court", false)
            ));

            assertThat(result.getOtherReasonableAdjustmentDetails()).isNull();
        }
    }

    @Nested
    class InterpreterLanguage {

        @Test
        void shouldUseSpokenLanguageKeyWhenChosenFromList() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("PF0015", "Language Interpreter", "kor", "Korean", null)
            ));

            assertThat(result.getInterpreterLanguage()).isEqualTo("kor");
        }

        @Test
        void shouldUseSignLanguageKeyWhenChosenFromList() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("RA0042", "Sign Language Interpreter", "sign-sse", "Sign Supported English (SSE)", null)
            ));

            assertThat(result.getInterpreterLanguage()).isEqualTo("sign-sse");
        }

        @Test
        void shouldPreferSpokenLanguageWhenBothInterpretersChosenFromList() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("RA0042", "Sign Language Interpreter", "sign-sse", "Sign Supported English (SSE)", null),
                languageFlag("PF0015", "Language Interpreter", "kor", "Korean", null)
            ));

            assertThat(result.getInterpreterLanguage()).isEqualTo("kor");
        }

        @Test
        void shouldNotSetLanguageWhenEnteredManually() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("PF0015", "Language Interpreter", null, "Latin", null)
            ));

            assertThat(result.getInterpreterLanguage()).isNull();
        }

        @Test
        void shouldNotSetLanguageWhenInterpreterFlagHasAComment() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("PF0015", "Language Interpreter", "kor", "Korean", "interpreter comment")
            ));

            assertThat(result.getInterpreterLanguage()).isNull();
        }

        @Test
        void shouldIgnoreInactiveInterpreterFlag() {
            CasePartyFlagEntity inactive = languageFlag("PF0015", "Language Interpreter", "kor", "Korean", null);
            inactive.setDefaultStatus("Inactive");

            IndividualDetails result = underTest.map(List.of(inactive));

            assertThat(result.getInterpreterLanguage()).isNull();
        }
    }

    @Nested
    class ReasonableAdjustments {

        @Test
        void shouldIncludeHearingRelevantUncommentedRaAndSmCodesSorted() {
            IndividualDetails result = underTest.map(List.of(
                flag("SM0003", "Evidence by live link", "Active", null, true),
                flag("RA0046", "Visit to court or tribunal before the hearing", "Active", null, true),
                flag("RA0019", "Step free / wheelchair access", "Active", null, true)
            ));

            assertThat(result.getReasonableAdjustments()).containsExactly("RA0019", "RA0046", "SM0003");
        }

        @Test
        void shouldMoveCommentedFlagToOtherDetailsInsteadOfReasonableAdjustments() {
            IndividualDetails result = underTest.map(List.of(
                flag("RA0019", "Step free / wheelchair access", "Active", "adjustment comment", true)
            ));

            assertThat(result.getReasonableAdjustments()).isNull();
            assertThat(result.getOtherReasonableAdjustmentDetails())
                .isEqualTo("Reasonable Adjustments: Step free / wheelchair access (Comment: adjustment comment).");
        }

        @Test
        void shouldExcludeFlagsThatAreNotHearingRelevant() {
            boolean notHearingRelevant = false;

            IndividualDetails result = underTest.map(List.of(
                flag("RA0019", "Step free / wheelchair access", "Active", null, notHearingRelevant)
            ));

            assertThat(result.getReasonableAdjustments()).isNull();
        }

        @Test
        void shouldExcludeSignLanguageInterpreter() {
            IndividualDetails result = underTest.map(List.of(
                flag("RA0042", "Sign Language Interpreter", "Active", null, true)
            ));

            assertThat(result.getReasonableAdjustments()).isNull();
        }
    }

    @Nested
    class OtherReasonableAdjustmentDetails {

        @Test
        void shouldGroupFlagsByCategorySortedByCodeWithComments() {
            IndividualDetails result = underTest.map(List.of(
                flag("PF0019", "Detained individual", "Active", null, false),
                flag("SM0003", "Evidence by live link", "Active", "special measure comment", true),
                flag("RA0046", "Visit to court or tribunal before the hearing", "Active", "adjustment comment", true),
                flag("PF0016", "Death of a Party", "Active", null, false)
            ));

            assertThat(result.getOtherReasonableAdjustmentDetails()).isEqualTo(
                "Reasonable Adjustments: Visit to court or tribunal before the hearing (Comment: adjustment comment).\n"
                    + "Special Measures: Evidence by live link (Comment: special measure comment).\n"
                    + "Party Flags: Death of a Party, Detained individual."
            );
        }

        @Test
        void shouldOnlyIncludeRaAndSmFlagsThatHaveAComment() {
            IndividualDetails result = underTest.map(List.of(
                flag("RA0019", "Step free / wheelchair access", "Active", null, true),
                flag("SM0003", "Evidence by live link", "Active", null, true)
            ));

            assertThat(result.getOtherReasonableAdjustmentDetails()).isNull();
        }

        @Test
        void shouldIncludePartyFlagsWithAndWithoutComments() {
            IndividualDetails result = underTest.map(List.of(
                flag("PF0019", "Detained individual", "Active", null, false),
                flag("PF0016", "Death of a Party", "Active", "Claimant deceased", false)
            ));

            assertThat(result.getOtherReasonableAdjustmentDetails())
                .isEqualTo("Party Flags: Death of a Party (Comment: Claimant deceased), Detained individual.");
        }

        @Test
        void shouldNotListInterpreterChosenFromListWithoutComment() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("PF0015", "Language Interpreter", "kor", "Korean", null)
            ));

            assertThat(result.getInterpreterLanguage()).isEqualTo("kor");
            assertThat(result.getOtherReasonableAdjustmentDetails()).isNull();
        }

        @Test
        void shouldListInterpreterChosenFromListWithComment() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("PF0015", "Language Interpreter", "kor", "Korean", "interpreter comment")
            ));

            assertThat(result.getOtherReasonableAdjustmentDetails()).isEqualTo(
                "Party Flags: Language Interpreter (Language selected: Korean, Comment: interpreter comment).");
        }

        @Test
        void shouldListManuallyEnteredLanguage() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("PF0015", "Language Interpreter", null, "Latin", null)
            ));

            assertThat(result.getOtherReasonableAdjustmentDetails())
                .isEqualTo("Party Flags: Language Interpreter (Language entered: Latin).");
        }

        @Test
        void shouldListManuallyEnteredLanguageWithComment() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("PF0015", "Language Interpreter", null, "Latin", "interpreter comment")
            ));

            assertThat(result.getOtherReasonableAdjustmentDetails()).isEqualTo(
                "Party Flags: Language Interpreter (Language entered: Latin, Comment: interpreter comment).");
        }

        @Test
        void shouldListSignLanguageInterpreterUnderPartyFlagsSortedByCode() {
            IndividualDetails result = underTest.map(List.of(
                languageFlag("RA0042", "Sign Language Interpreter", null, "Makaton", null),
                flag("PF0019", "Detained individual", "Active", null, false),
                flag("RA0019", "Step free / wheelchair access", "Active", "adjustment comment", true)
            ));

            assertThat(result.getOtherReasonableAdjustmentDetails()).isEqualTo(
                "Reasonable Adjustments: Step free / wheelchair access (Comment: adjustment comment).\n"
                    + "Party Flags: Detained individual, Sign Language Interpreter (Language entered: Makaton).");
        }

        @Test
        void shouldTruncateToHmcMaximumLength() {
            String longComment = "x".repeat(255);
            List<CasePartyFlagEntity> flags = List.of(
                "PF0004", "PF0007", "PF0008", "PF0009", "PF0011", "PF0012", "PF0013",
                "PF0014", "PF0016", "PF0017", "PF0018", "PF0019", "PF0020"
            ).stream().map(code -> flag(code, "Party flag " + code, "Active", longComment, false)).toList();

            IndividualDetails result = underTest.map(flags);

            assertThat(result.getOtherReasonableAdjustmentDetails())
                .hasSize(IndividualDetailsFlagMapper.OTHER_DETAILS_MAX_LENGTH)
                .startsWith("Party Flags: Party flag PF0004")
                .endsWith(IndividualDetailsFlagMapper.TRUNCATION_MARKER);
        }
    }

    private static CasePartyFlagEntity flag(String code, String name, String status, String comment,
                                            boolean hearingRelevant) {
        CasePartyFlagEntity flag = new CasePartyFlagEntity();
        flag.setFlagRefData(FlagRefDataEntity.builder()
            .flagCode(code)
            .flagName(name)
            .hearingRelevant(hearingRelevant)
            .build());
        flag.setDefaultStatus(status);
        flag.setFlagComment(comment);
        return flag;
    }

    private static CasePartyFlagEntity languageFlag(String code, String name, String languageKey, String language,
                                                    String comment) {
        CasePartyFlagEntity flag = flag(code, name, "Active", comment, true);
        flag.setSubTypeKey(languageKey);
        flag.setSubTypeValue(language);
        return flag;
    }
}
