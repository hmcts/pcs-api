package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SIGN_LANGUAGE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SIGN_LANGUAGE_INTERPRETER_FLAG_CODE;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SPOKEN_LANGUAGE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE;

@ExtendWith(MockitoExtension.class)
class LanguageFlagDuplicateValidatorTest {

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @InjectMocks
    private LanguageFlagDuplicateValidator underTest;

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

    @Test
    void shouldReturnValidationResponseForFlagsPage() {
        PCSCase caseData = PCSCase.builder()
            .parties(List.of(party(
                flags(flag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                    flag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active")),
                null)))
            .build();
        List<String> duplicateErrors = List.of(SPOKEN_LANGUAGE_ERROR);
        AboutToStartOrSubmitResponse<PCSCase, State> validationResponse =
            AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
        when(textAreaValidationService.<PCSCase, State>createValidationResponse(caseData, duplicateErrors))
            .thenReturn(validationResponse);

        AboutToStartOrSubmitResponse<PCSCase, State> response = underTest.validateFlagsPage(
            CaseDetails.<PCSCase, State>builder().data(caseData).build(), null);

        assertThat(response).isSameAs(validationResponse);
        verify(textAreaValidationService).createValidationResponse(caseData, duplicateErrors);
    }

    @Test
    void shouldReturnValidationResponseWithNoErrorsForFlagsPage() {
        PCSCase caseData = PCSCase.builder().build();
        List<String> noErrors = List.of();
        AboutToStartOrSubmitResponse<PCSCase, State> validationResponse =
            AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData).build();
        when(textAreaValidationService.<PCSCase, State>createValidationResponse(caseData, noErrors))
            .thenReturn(validationResponse);

        AboutToStartOrSubmitResponse<PCSCase, State> response = underTest.validateFlagsPage(
            CaseDetails.<PCSCase, State>builder().data(caseData).build(), null);

        assertThat(response).isSameAs(validationResponse);
        verify(textAreaValidationService).createValidationResponse(caseData, noErrors);
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
