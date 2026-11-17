package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SIGN_LANGUAGE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SIGN_LANGUAGE_INTERPRETER_FLAG_CODE;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SPOKEN_LANGUAGE_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator.SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE;

class LanguageFlagDuplicateValidatorTest {

    private static final String OTHER_FLAG_CODE = "PF0002";

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
    @MethodSource("languageFlags")
    void shouldRejectTwoActiveFlagsOfTheSameCodeOnOnePerson(String flagCode, String expectedError) {
        UUID partyId = UUID.randomUUID();
        PartyEntity existingParty = existingParty(partyId);

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(incomingFlag(flagCode, "Active"), incomingFlag(flagCode, "Active")),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).containsExactly(expectedError);
    }

    @ParameterizedTest
    @MethodSource("languageFlags")
    void shouldRejectAnActiveCaseworkerFlagNextToARequestedCitizenFlag(String flagCode, String expectedError) {
        UUID partyId = UUID.randomUUID();
        PartyEntity existingParty = existingParty(partyId, existingFlag(flagCode, "Requested", "External"));

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(incomingFlag(flagCode, "Active")),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).containsExactly(expectedError);
    }

    @ParameterizedTest
    @MethodSource("languageFlagCodes")
    void shouldAllowAnActiveFlagNextToAnInactiveOne(String flagCode) {
        UUID partyId = UUID.randomUUID();
        PartyEntity existingParty = existingParty(partyId);

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(incomingFlag(flagCode, "Inactive"), incomingFlag(flagCode, "Active")),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("languageFlagCodes")
    void shouldAllowAnActiveFlagNextToANotApprovedOne(String flagCode) {
        UUID partyId = UUID.randomUUID();
        PartyEntity existingParty = existingParty(partyId);

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(incomingFlag(flagCode, "Not approved"), incomingFlag(flagCode, "Active")),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAllowOneSpokenAndOneSignLanguageFlagOnOnePerson() {
        UUID partyId = UUID.randomUUID();
        PartyEntity existingParty = existingParty(partyId);

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(
                incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(SIGN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active")
            ),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("languageFlagCodes")
    void shouldAllowTheSameFlagOnDifferentPeople(String flagCode) {
        UUID firstPartyId = UUID.randomUUID();
        UUID secondPartyId = UUID.randomUUID();

        List<ListValue<Party>> incomingParties = List.of(
            incomingParty(firstPartyId, flags(incomingFlag(flagCode, "Active")), null),
            incomingParty(secondPartyId, flags(incomingFlag(flagCode, "Active")), null)
        );

        List<String> errors = underTest.validatePartyLanguageFlags(
            incomingParties, Set.of(existingParty(firstPartyId), existingParty(secondPartyId)));

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAllowDeactivatingOneOfTwoExistingDuplicates() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity firstFlag = existingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active", "Internal");
        CasePartyFlagEntity secondFlag = existingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active", "Internal");
        PartyEntity existingParty = existingParty(partyId, firstFlag, secondFlag);

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(
                incomingFlag(firstFlag.getId(), SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(secondFlag.getId(), SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Inactive")
            ),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldAllowChangingAnotherFlagWhenDuplicatesAlreadyExist() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity firstFlag = existingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active", "Internal");
        CasePartyFlagEntity secondFlag = existingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active", "Internal");
        PartyEntity existingParty = existingParty(partyId, firstFlag, secondFlag);

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(
                incomingFlag(firstFlag.getId(), SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(secondFlag.getId(), SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(OTHER_FLAG_CODE, "Active")
            ),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldRejectAThirdFlagWhenDuplicatesAlreadyExist() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity firstFlag = existingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active", "Internal");
        CasePartyFlagEntity secondFlag = existingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active", "Internal");
        PartyEntity existingParty = existingParty(partyId, firstFlag, secondFlag);

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(
                incomingFlag(firstFlag.getId(), SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(secondFlag.getId(), SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active")
            ),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).containsExactly(SPOKEN_LANGUAGE_ERROR);
    }

    @Test
    void shouldReturnBothErrorsWhenBothFlagsAreDuplicated() {
        UUID partyId = UUID.randomUUID();
        PartyEntity existingParty = existingParty(partyId);

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(
                incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(SIGN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                incomingFlag(SIGN_LANGUAGE_INTERPRETER_FLAG_CODE, "Requested")
            ),
            null
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).containsExactly(SPOKEN_LANGUAGE_ERROR, SIGN_LANGUAGE_ERROR);
    }

    @Test
    void shouldReturnEachErrorOnceWhenSeveralPeopleHaveDuplicates() {
        UUID firstPartyId = UUID.randomUUID();
        UUID secondPartyId = UUID.randomUUID();

        List<ListValue<Party>> incomingParties = List.of(
            incomingParty(
                firstPartyId,
                flags(
                    incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                    incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active")
                ),
                null
            ),
            incomingParty(
                secondPartyId,
                flags(
                    incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active"),
                    incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Requested")
                ),
                null
            )
        );

        List<String> errors = underTest.validatePartyLanguageFlags(
            incomingParties, Set.of(existingParty(firstPartyId), existingParty(secondPartyId)));

        assertThat(errors).containsExactly(SPOKEN_LANGUAGE_ERROR);
    }

    @Test
    void shouldCountSavedCitizenFlagsWhenOnlyCaseworkerFlagsAreSent() {
        UUID partyId = UUID.randomUUID();
        PartyEntity existingParty = existingParty(
            partyId,
            existingFlag(SIGN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active", "External")
        );

        ListValue<Party> incomingParty = incomingParty(
            partyId,
            flags(incomingFlag(SIGN_LANGUAGE_INTERPRETER_FLAG_CODE, "Requested")),
            flags()
        );

        List<String> errors = underTest.validatePartyLanguageFlags(List.of(incomingParty), Set.of(existingParty));

        assertThat(errors).containsExactly(SIGN_LANGUAGE_ERROR);
    }

    @Test
    void shouldReturnNoErrorsWhenThereAreNoIncomingParties() {
        assertThat(underTest.validatePartyLanguageFlags(null, Set.of())).isEmpty();
        assertThat(underTest.validatePartyLanguageFlags(List.of(), Set.of())).isEmpty();
    }

    @Test
    void shouldIgnoreMissingValuesWithoutThrowing() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity flagWithoutRefData = existingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, "Active", null);
        flagWithoutRefData.setFlagRefData(null);
        PartyEntity existingParty = existingParty(partyId, flagWithoutRefData);

        List<ListValue<Party>> incomingParties = new ArrayList<>();
        incomingParties.add(null);
        incomingParties.add(ListValue.<Party>builder().id(null).value(Party.builder().build()).build());
        incomingParties.add(ListValue.<Party>builder().id("not-a-uuid").value(Party.builder().build()).build());
        incomingParties.add(ListValue.<Party>builder().id(UUID.randomUUID().toString()).build());
        incomingParties.add(ListValue.<Party>builder().id(partyId.toString()).value(null).build());
        incomingParties.add(incomingParty(
            partyId,
            Flags.builder().details(null).build(),
            flags(
                null,
                ListValue.<FlagDetail>builder().id(UUID.randomUUID().toString()).value(null).build(),
                incomingFlag(SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE, null),
                incomingFlag(null, "Active")
            )
        ));

        List<String> errors = underTest.validatePartyLanguageFlags(incomingParties, Set.of(existingParty));

        assertThat(errors).isEmpty();
    }

    private PartyEntity existingParty(UUID partyId, CasePartyFlagEntity... flags) {
        return PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(flags)))
            .build();
    }

    private CasePartyFlagEntity existingFlag(String flagCode, String status, String visibility) {
        CasePartyFlagEntity flag = new CasePartyFlagEntity();
        flag.setId(UUID.randomUUID());
        flag.setFlagRefData(FlagRefDataEntity.builder().flagCode(flagCode).build());
        flag.setDefaultStatus(status);
        flag.setVisibility(visibility);
        return flag;
    }

    private ListValue<Party> incomingParty(UUID partyId, Flags caseworkerFlags, Flags citizenFlags) {
        return ListValue.<Party>builder()
            .id(partyId.toString())
            .value(Party.builder()
                       .defendantFlags(caseworkerFlags)
                       .partyFlagsExternal(citizenFlags)
                       .build())
            .build();
    }

    @SafeVarargs
    private Flags flags(ListValue<FlagDetail>... details) {
        return Flags.builder()
            .details(new ArrayList<>(Arrays.asList(details)))
            .build();
    }

    private ListValue<FlagDetail> incomingFlag(String flagCode, String status) {
        return incomingFlag(UUID.randomUUID(), flagCode, status);
    }

    private ListValue<FlagDetail> incomingFlag(UUID id, String flagCode, String status) {
        return ListValue.<FlagDetail>builder()
            .id(id.toString())
            .value(FlagDetail.builder()
                       .flagCode(flagCode)
                       .status(status)
                       .build())
            .build();
    }
}
