package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDocumentRef;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeFormDocumentGenerator;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimPackSelectorTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    @Mock
    private FeatureToggleService featureToggleService;

    @Spy
    private SentPackDocuments sentPackDocuments = new SentPackDocuments(new ObjectMapper());

    private ClaimPackSelector underTest;

    @BeforeEach
    void setUp() {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(false);
        underTest = new ClaimPackSelector(
            claimActivityLogRepository,
            sentPackDocuments,
            new PackSkipRules(featureToggleService, new CaseFlagService(null, null, null, null, null),
                Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC), Duration.ofHours(24)));
    }

    private final PartyEntity claimant = party();
    private final PartyEntity defendantA = party();
    private final PartyEntity defendantB = party();
    private final PartyEntity defendantC = party();
    private final DocumentEntity claimForm = document(DocumentType.CLAIM, null);

    @Test
    @DisplayName("Returns nothing when there is no claim form")
    void shouldReturnNothingWhenNoClaimForm() {
        PcsCaseEntity pcsCase = caseWith(null, List.of(), List.of(claimParty(claimant, PartyRole.CLAIMANT, 1)));

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Includes claimant and a defendant whose access code is ready")
    void shouldIncludeClaimantAndReadyDefendant() {
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(pinA), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1)));

        List<ClaimPackCandidate> result = underTest.findClaimPackCandidates(pcsCase);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().recipientType()).isEqualTo(PartyRole.CLAIMANT);
        assertThat(result.getFirst().documents()).containsExactly(claimForm);
        assertThat(result.get(1).recipientType()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(result.get(1).party()).isEqualTo(defendantA);
        assertThat(result.get(1).documents()).containsExactly(pinA, claimForm);
    }

    @Test
    @DisplayName("Excludes a claimant already sent the claim form")
    void shouldExcludeClaimantAlreadySent() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID))
            .thenReturn(List.of(sent(claimant, claimForm)));
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(), List.of(claimParty(claimant, PartyRole.CLAIMANT, 1)));

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Re-sends only the access code when the claim form already reached the defendant")
    void shouldResendOnlyAccessCodeWhenClaimFormAlreadySent() {
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID))
            .thenReturn(List.of(sent(claimant, claimForm), sent(defendantA, claimForm)));
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(pinA), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1)));

        List<ClaimPackCandidate> result = underTest.findClaimPackCandidates(pcsCase);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().party()).isEqualTo(defendantA);
        assertThat(result.getFirst().documents()).containsExactly(pinA);   // claim form not re-sent
    }

    @Test
    @DisplayName("Sends to the ready defendant and holds the one whose access code is missing")
    void shouldHandleMultipleDefendantsIndependently() {
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity pinC = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantC);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            sent(claimant, claimForm),
            sent(defendantC, claimForm), sent(defendantC, pinC)));
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(pinA, pinC), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1),   // pin, unsent  → included (ready)
            claimParty(defendantB, PartyRole.DEFENDANT, 2),   // no pin       → held
            claimParty(defendantC, PartyRole.DEFENDANT, 3))); // pin, sent    → excluded

        List<ClaimPackCandidate> result = underTest.findClaimPackCandidates(pcsCase);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().party()).isEqualTo(defendantA);
        assertThat(result.getFirst().documents()).containsExactly(pinA, claimForm);
    }

    @Test
    @DisplayName("Excludes removed parties from claim pack recipients")
    void shouldExcludeRemovedPartiesFromClaimPackRecipients() {
        defendantB.setRemoved(true);
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity pinB = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantB);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(pinA, pinB), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1),
            claimParty(defendantB, PartyRole.DEFENDANT, 2)));

        List<ClaimPackCandidate> result = underTest.findClaimPackCandidates(pcsCase);

        assertThat(result)
            .extracting(ClaimPackCandidate::party)
            .containsExactly(claimant, defendantA);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    @DisplayName("Does not send the claim pack when the claim requires translation")
    void shouldSkipClaimPackWhenTranslationRequired(LanguageUsed languageUsed) {
        enableRelease13();
        PcsCaseEntity pcsCase = readyClaimCase();
        pcsCase.getClaims().getFirst().setLanguageUsed(languageUsed);
        pcsCase.getClaims().getFirst().setGenAppExpected(VerticalYesNo.NO);

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Does not send the claim pack when a gen app is expected")
    void shouldSkipClaimPackWhenGenAppExpected() {
        enableRelease13();
        PcsCaseEntity pcsCase = readyClaimCase();
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);
        pcsCase.getClaims().getFirst().setGenAppExpected(VerticalYesNo.YES);

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Does not send the claim pack when translation and a gen app are both required")
    void shouldSkipClaimPackWhenTranslationAndGenAppExpected() {
        enableRelease13();
        PcsCaseEntity pcsCase = readyClaimCase();
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.WELSH);
        pcsCase.getClaims().getFirst().setGenAppExpected(VerticalYesNo.YES);

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Still sends an English claim with no expected gen app when the rollout flag is on")
    void shouldStillSendEnglishClaimWhenRolloutFlagOn() {
        enableRelease13();
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = readyClaimCase();
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);
        pcsCase.getClaims().getFirst().setGenAppExpected(VerticalYesNo.NO);

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isNotEmpty();
    }

    @Test
    @DisplayName("Sends a Welsh claim when the rollout flag is off")
    void shouldSendWelshClaimWhenRolloutFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = readyClaimCase();
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.WELSH);

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isNotEmpty();
    }

    @Test
    @DisplayName("Does not send the claim pack when an Active PF0026 case flag is present")
    void shouldSkipClaimPackWhenActiveCaseFlag() {
        enableRelease13();
        PcsCaseEntity pcsCase = readyClaimCase();
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);
        CaseFlagEntity flag = new CaseFlagEntity();
        flag.setDefaultStatus("Active");
        flag.setFlagRefData(FlagRefDataEntity.builder()
            .flagCode("PF0026").build());
        pcsCase.setCaseFlags(List.of(flag));

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Does not send the claim pack when an Active PF0026 party flag is present")
    void shouldSkipClaimPackWhenActivePartyFlag() {
        enableRelease13();
        CasePartyFlagEntity flag = new CasePartyFlagEntity();
        flag.setDefaultStatus("Active");
        flag.setFlagRefData(FlagRefDataEntity.builder()
            .flagCode("PF0026").build());
        PartyEntity flaggedClaimant = party();
        flaggedClaimant.getDefendantFlags().add(flag);
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(pinA), List.of(
            claimParty(flaggedClaimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1)));
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Still sends when PF0026 is inactive")
    void shouldSendClaimPackWhenTranslationFlagInactive() {
        enableRelease13();
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = readyClaimCase();
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);
        CaseFlagEntity flag = new CaseFlagEntity();
        flag.setDefaultStatus("Inactive");
        flag.setFlagRefData(FlagRefDataEntity.builder()
            .flagCode("PF0026").build());
        pcsCase.setCaseFlags(List.of(flag));

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isNotEmpty();
    }

    @Test
    @DisplayName("Leaves an already sent claim pack untouched")
    void shouldLeaveAlreadySentClaimPackUntouchedWhenSkipWouldApply() {
        enableRelease13();
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID))
            .thenReturn(List.of(sent(claimant, claimForm)));
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(), List.of(claimParty(claimant, PartyRole.CLAIMANT, 1)));
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    @DisplayName("Release 1.4 excludes claimant from receiving claim pack without changing defendant receiving pack")
    void shouldExcludeClaimantFromReceivingClaimPack(boolean release14Enabled, boolean release13Enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(release14Enabled);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(release13Enabled);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = readyClaimCase();

        List<ClaimPackCandidate> result = underTest.findClaimPackCandidates(pcsCase);

        assertThat(result).extracting(ClaimPackCandidate::party)
            .containsExactlyElementsOf(release14Enabled ? List.of(defendantA) : List.of(claimant, defendantA));
        assertThat(result.getLast().recipientType()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(result.getLast().documents()).containsExactly(pcsCase.getDocuments().getFirst(), claimForm);
    }

    @Test
    @DisplayName("Sends the English access-code letter, then the Welsh one, then the claim form on a Wales case")
    void shouldSendEnglishThenWelshAccessCodeLetterThenClaimFormOnAWalesCase() {
        DocumentEntity englishPinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity welshPinA = welshPin(defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = walesCaseWith(List.of(welshPinA, englishPinA), claimantAnd(defendantA));

        assertThat(defendantCandidates(pcsCase)).singleElement().satisfies(candidate -> {
            assertThat(candidate.party()).isEqualTo(defendantA);
            assertThat(candidate.documents()).containsExactly(englishPinA, welshPinA, claimForm);
        });
    }

    @Test
    @DisplayName("Holds a Wales defendant who has only the English access-code letter")
    void shouldHoldAWalesDefendantWithOnlyTheEnglishAccessCodeLetter() {
        DocumentEntity englishPinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = walesCaseWith(List.of(englishPinA), claimantAnd(defendantA));

        assertThat(defendantCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Holds a Wales defendant who has only the Welsh access-code letter")
    void shouldHoldAWalesDefendantWithOnlyTheWelshAccessCodeLetter() {
        DocumentEntity welshPinA = welshPin(defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = walesCaseWith(List.of(welshPinA), claimantAnd(defendantA));

        assertThat(defendantCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Re-sends only the Welsh access-code letter when the English one and the claim form "
        + "already reached the defendant")
    void shouldResendOnlyTheWelshAccessCodeLetter() {
        DocumentEntity englishPinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity welshPinA = welshPin(defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID))
            .thenReturn(List.of(sent(defendantA, englishPinA), sent(defendantA, claimForm)));
        PcsCaseEntity pcsCase = walesCaseWith(List.of(englishPinA, welshPinA), claimantAnd(defendantA));

        assertThat(defendantCandidates(pcsCase)).singleElement()
            .satisfies(candidate -> assertThat(candidate.documents()).containsExactly(welshPinA));
    }

    @Test
    @DisplayName("Sends nothing more to a Wales defendant once the whole pack was sent")
    void shouldSendNothingMoreOnceTheWholeWalesPackWasSent() {
        DocumentEntity englishPinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity welshPinA = welshPin(defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            sent(defendantA, englishPinA), sent(defendantA, welshPinA), sent(defendantA, claimForm)));
        PcsCaseEntity pcsCase = walesCaseWith(List.of(englishPinA, welshPinA), claimantAnd(defendantA));

        assertThat(defendantCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Gives each Wales defendant their own English and Welsh access-code letters")
    void shouldGiveEachWalesDefendantTheirOwnAccessCodeLetters() {
        DocumentEntity englishPinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity welshPinA = welshPin(defendantA);
        DocumentEntity englishPinB = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantB);
        DocumentEntity welshPinB = welshPin(defendantB);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = walesCaseWith(List.of(welshPinB, englishPinA, englishPinB, welshPinA), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1),
            claimParty(defendantB, PartyRole.DEFENDANT, 2)));

        List<ClaimPackCandidate> result = defendantCandidates(pcsCase);

        assertThat(result).extracting(ClaimPackCandidate::party).containsExactly(defendantA, defendantB);
        assertThat(result.get(0).documents()).containsExactly(englishPinA, welshPinA, claimForm);
        assertThat(result.get(1).documents()).containsExactly(englishPinB, welshPinB, claimForm);
    }

    @Test
    @DisplayName("Sends a Wales claim in English with no flag and no expected gen app "
        + "once both access-code letters exist")
    void shouldSendAWalesEnglishLanguageClaimWithBothAccessCodeLetters() {
        enableRelease13();
        DocumentEntity englishPinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity welshPinA = welshPin(defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = walesCaseWith(List.of(englishPinA, welshPinA), claimantAnd(defendantA));
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);
        pcsCase.getClaims().getFirst().setGenAppExpected(VerticalYesNo.NO);

        assertThat(defendantCandidates(pcsCase)).singleElement()
            .satisfies(candidate -> assertThat(candidate.documents())
                .containsExactly(englishPinA, welshPinA, claimForm));
    }

    @Test
    @DisplayName("Does not send a Wales claim pack when an Active PF0026 case flag is present")
    void shouldSkipAWalesClaimPackWithAnActiveTranslationFlag() {
        enableRelease13();
        PcsCaseEntity pcsCase = walesCaseWith(
            List.of(document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA), welshPin(defendantA)),
            claimantAnd(defendantA));
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);
        CaseFlagEntity flag = new CaseFlagEntity();
        flag.setDefaultStatus("Active");
        flag.setFlagRefData(FlagRefDataEntity.builder()
            .flagCode("PF0026").build());
        pcsCase.setCaseFlags(List.of(flag));

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Holds a Wales claim pack while a gen app is expected")
    void shouldHoldAWalesClaimPackWhileAGenAppIsExpected() {
        enableRelease13();
        PcsCaseEntity pcsCase = walesCaseWith(
            List.of(document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA), welshPin(defendantA)),
            claimantAnd(defendantA));
        pcsCase.getClaims().getFirst().setLanguageUsed(LanguageUsed.ENGLISH);
        pcsCase.getClaims().getFirst().setGenAppExpected(VerticalYesNo.YES);

        assertThat(underTest.findClaimPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Gives a Wales claimant only the claim form")
    void shouldGiveAWalesClaimantOnlyTheClaimForm() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = walesCaseWith(
            List.of(document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA), welshPin(defendantA)),
            claimantAnd(defendantA));

        assertThat(underTest.findClaimPackCandidates(pcsCase))
            .filteredOn(candidate -> candidate.recipientType() == PartyRole.CLAIMANT)
            .singleElement()
            .satisfies(candidate -> assertThat(candidate.documents()).containsExactly(claimForm));
    }

    @Test
    @DisplayName("Ignores a Welsh-marked access-code letter on an England case")
    void shouldIgnoreAWelshMarkedAccessCodeLetterOnAnEnglandCase() {
        DocumentEntity englishPinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        DocumentEntity welshPinA = welshPin(defendantA);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = caseWith(claimForm, List.of(welshPinA, englishPinA), claimantAnd(defendantA));
        pcsCase.setLegislativeCountry(LegislativeCountry.ENGLAND);

        assertThat(defendantCandidates(pcsCase)).singleElement()
            .satisfies(candidate -> assertThat(candidate.documents()).containsExactly(englishPinA, claimForm));
    }

    @ParameterizedTest(name = "{0}, English letter {1}, Welsh letter {2} -> awaiting {3}")
    @CsvSource({
        "WALES, true, false, Welsh access-code letter",
        "WALES, false, true, English access-code letter",
        "WALES, false, false, access code (English and Welsh letters)",
        "ENGLAND, false, false, access code",
        "ENGLAND, false, true, access code"
    })
    @DisplayName("Names the missing access-code letter when a defendant's pack is held")
    void shouldNameTheMissingLetterWhenHoldingADefendantPack(LegislativeCountry country, boolean hasEnglishPin,
                                                             boolean hasWelshPin, String awaiting) {
        List<DocumentEntity> documents = new ArrayList<>();
        if (hasEnglishPin) {
            documents.add(document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA));
        }
        if (hasWelshPin) {
            documents.add(welshPin(defendantA));
        }
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        PcsCaseEntity pcsCase = caseWith(claimForm, documents, claimantAnd(defendantA));
        pcsCase.setLegislativeCountry(country);

        Logger selectorLogger = (Logger) LoggerFactory.getLogger(ClaimPackSelector.class);
        Level previousLevel = selectorLogger.getLevel();
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        selectorLogger.addAppender(logAppender);
        selectorLogger.setLevel(Level.DEBUG);
        try {
            assertThat(defendantCandidates(pcsCase)).isEmpty();
        } finally {
            selectorLogger.detachAppender(logAppender);
            selectorLogger.setLevel(previousLevel);
        }

        assertThat(logAppender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo(
                "Claim pack held for case " + CASE_ID + " defendant " + defendantA.getId() + " - awaiting " + awaiting);
        });
    }

    private PcsCaseEntity readyClaimCase() {
        DocumentEntity pinA = document(DocumentType.DEFENDANT_ACCESS_CODE, defendantA);
        return caseWith(claimForm, List.of(pinA), List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendantA, PartyRole.DEFENDANT, 1)));
    }

    private void enableRelease13() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
    }

    private PcsCaseEntity caseWith(DocumentEntity claimFormDocument, List<DocumentEntity> documents,
                                   List<ClaimPartyEntity> claimParties) {
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(claimParties)
            .claimFormDocument(claimFormDocument)
            .build();
        return PcsCaseEntity.builder().id(CASE_ID).claims(List.of(claim)).documents(documents).build();
    }

    private ClaimPartyEntity claimParty(PartyEntity party, PartyRole role, int rank) {
        return ClaimPartyEntity.builder().party(party).role(role).rank(rank).build();
    }

    private ClaimActivityLogEntity sent(PartyEntity party, DocumentEntity document) {
        try {
            String details = new ObjectMapper().writeValueAsString(PackDetails.sent(
                LetterType.CLAIMANT_CLAIM_PACK,
                List.of(new PackDocumentRef(document.getId(), document.getType(), null, false)),
                UUID.randomUUID()));
            return ClaimActivityLogEntity.builder()
                .party(party).details(details)
                .activityType(ClaimActivityType.PACK_SENT).status(ClaimActivityStatus.SUCCESS).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }

    private DocumentEntity document(DocumentType type, PartyEntity party) {
        return DocumentEntity.builder().id(UUID.randomUUID()).type(type).party(party).build();
    }

    private DocumentEntity welshPin(PartyEntity party) {
        DocumentEntity pin = document(DocumentType.DEFENDANT_ACCESS_CODE, party);
        pin.setFileName(AccessCodeFormDocumentGenerator.welshFileName(party.getId()));
        return pin;
    }

    private PcsCaseEntity walesCaseWith(List<DocumentEntity> documents, List<ClaimPartyEntity> claimParties) {
        PcsCaseEntity pcsCase = caseWith(claimForm, documents, claimParties);
        pcsCase.setLegislativeCountry(LegislativeCountry.WALES);
        return pcsCase;
    }

    private List<ClaimPartyEntity> claimantAnd(PartyEntity defendant) {
        return List.of(
            claimParty(claimant, PartyRole.CLAIMANT, 1),
            claimParty(defendant, PartyRole.DEFENDANT, 1));
    }

    private List<ClaimPackCandidate> defendantCandidates(PcsCaseEntity pcsCase) {
        return underTest.findClaimPackCandidates(pcsCase).stream()
            .filter(candidate -> candidate.recipientType() == PartyRole.DEFENDANT)
            .toList();
    }
}
