package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SIGN_LANGUAGE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SIGN_LANGUAGE_INTERPRETER_FLAG_CODE;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SPOKEN_LANGUAGE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE;

class LanguageFlagDuplicateValidatorTest {

    private final LanguageFlagDuplicateValidator underTest = new LanguageFlagDuplicateValidator();

    static Stream<Arguments> languageFlags() {
        return Stream.of(
            Arguments.of(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, SPOKEN_LANGUAGE_ERROR),
            Arguments.of(SIGN_LANGUAGE_INTERPRETER_FLAG_CODE, SIGN_LANGUAGE_ERROR)
        );
    }

    static Stream<String> languageFlagCodes() {
        return Stream.of(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, SIGN_LANGUAGE_INTERPRETER_FLAG_CODE);
    }

    @ParameterizedTest
    @MethodSource("languageFlagCodes")
    void shouldAllowOneFlag(String flagCode) {
        List<String> errors = underTest.validatePartyLanguageFlags(List.of(
            party(flags(flag(flagCode, "Active")), null)
        ));

        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("languageFlags")
    void shouldRejectTwoActiveFlags(String flagCode, String expectedError) {
        List<String> errors = underTest.validatePartyLanguageFlags(List.of(
            party(flags(flag(flagCode, "Active"), flag(flagCode, "Active")), null)
        ));

        assertThat(errors).containsExactly(expectedError);
    }

    @ParameterizedTest
    @MethodSource("languageFlagCodes")
    void shouldAllowAnActiveFlagNextToAnInactiveOne(String flagCode) {
        List<String> errors = underTest.validatePartyLanguageFlags(List.of(
            party(flags(flag(flagCode, "Active"), flag(flagCode, "Inactive")), null)
        ));

        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("languageFlags")
    void shouldRejectAnActiveFlagNextToARequestedOne(String flagCode, String expectedError) {
        List<String> errors = underTest.validatePartyLanguageFlags(List.of(
            party(flags(flag(flagCode, "Active"), flag(flagCode, "Requested")), null)
        ));

        assertThat(errors).containsExactly(expectedError);
    }

    @Test
    void shouldAllowOneSpokenAndOneSignLanguageFlag() {
        List<String> errors = underTest.validatePartyLanguageFlags(List.of(
            party(flags(
                flag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                flag(SIGN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active")
            ), null)
        ));

        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("languageFlags")
    void shouldRejectTheSameFlagInTheInternalAndExternalLists(String flagCode, String expectedError) {
        List<String> errors = underTest.validatePartyLanguageFlags(List.of(
            party(flags(flag(flagCode, "Active")), flags(flag(flagCode, "Requested")))
        ));

        assertThat(errors).containsExactly(expectedError);
    }

    @ParameterizedTest
    @MethodSource("languageFlagCodes")
    void shouldAllowTheSameFlagOnDifferentPeople(String flagCode) {
        List<String> errors = underTest.validatePartyLanguageFlags(List.of(
            party(flags(flag(flagCode, "Active")), null),
            party(flags(flag(flagCode, "Active")), null)
        ));

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldIgnoreMissingValuesWithoutThrowing() {
        List<ListValue<Party>> parties = new ArrayList<>();
        parties.add(null);
        parties.add(ListValue.<Party>builder().id(UUID.randomUUID().toString()).build());
        parties.add(party(Flags.builder().details(null).build(), null));
        parties.add(party(flags(
            null,
            ListValue.<FlagDetail>builder().id(UUID.randomUUID().toString()).build(),
            flag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, null),
            flag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, null),
            flag(null, "Active")
        ), null));

        assertThat(underTest.validatePartyLanguageFlags(null)).isEmpty();
        assertThat(underTest.validatePartyLanguageFlags(parties)).isEmpty();
    }

    private ListValue<Party> party(Flags internalFlags, Flags externalFlags) {
        return ListValue.<Party>builder()
            .id(UUID.randomUUID().toString())
            .value(Party.builder()
                       .defendantFlags(internalFlags)
                       .partyFlagsExternal(externalFlags)
                       .build())
            .build();
    }

    @SafeVarargs
    private Flags flags(ListValue<FlagDetail>... details) {
        return Flags.builder()
            .details(new ArrayList<>(Arrays.asList(details)))
            .build();
    }

    private ListValue<FlagDetail> flag(String flagCode, String status) {
        return ListValue.<FlagDetail>builder()
            .id(UUID.randomUUID().toString())
            .value(FlagDetail.builder()
                       .flagCode(flagCode)
                       .status(status)
                       .build())
            .build();
    }
}
