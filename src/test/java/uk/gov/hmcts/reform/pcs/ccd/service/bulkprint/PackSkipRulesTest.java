package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HelpWithFeesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackSkipRulesTest {

    @Mock
    private FeatureToggleService featureToggleService;

    private PackSkipRules underTest;

    @BeforeEach
    void setUp() {
        underTest = new PackSkipRules(
            featureToggleService, new CaseFlagService(null, null, null, null, null));
    }

    @Test
    @DisplayName("Does not skip any pack when the rollout flag is off")
    void shouldNotSkipWhenRolloutFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
        PcsCaseEntity pcsCase = welshCaseWithExpectedGenAppAndHwf();
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        GenAppEntity genApp = welshGenApp();

        assertThat(underTest.shouldSkipClaimPack(pcsCase, claim)).isFalse();
        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isFalse();
        assertThat(underTest.shouldSkipGenAppPack(pcsCase, genApp)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    @DisplayName("Skips the claim pack when the claim requires translation")
    void shouldSkipClaimPackWhenTranslationRequired(LanguageUsed languageUsed) {
        enableRelease13();
        ClaimEntity claim = ClaimEntity.builder().languageUsed(languageUsed).genAppExpected(VerticalYesNo.NO).build();
        PcsCaseEntity pcsCase = caseWith(claim);

        assertThat(underTest.shouldSkipClaimPack(pcsCase, claim)).isTrue();
    }

    @Test
    @DisplayName("Skips the claim pack when a gen app is expected and no translation is required")
    void shouldSkipClaimPackWhenGenAppExpected() {
        enableRelease13();
        ClaimEntity claim = ClaimEntity.builder()
            .languageUsed(LanguageUsed.ENGLISH)
            .genAppExpected(VerticalYesNo.YES)
            .build();
        PcsCaseEntity pcsCase = caseWith(claim);

        assertThat(underTest.shouldSkipClaimPack(pcsCase, claim)).isTrue();
    }

    @Test
    @DisplayName("Skips the claim pack when translation is required and a gen app is expected")
    void shouldSkipClaimPackWhenTranslationAndGenAppExpected() {
        enableRelease13();
        ClaimEntity claim = ClaimEntity.builder()
            .languageUsed(LanguageUsed.WELSH)
            .genAppExpected(VerticalYesNo.YES)
            .build();
        PcsCaseEntity pcsCase = caseWith(claim);

        assertThat(underTest.shouldSkipClaimPack(pcsCase, claim)).isTrue();
    }

    @Test
    @DisplayName("Does not skip an English claim with no expected gen app")
    void shouldNotSkipEnglishClaimWithNoGenAppExpected() {
        enableRelease13();
        ClaimEntity claim = ClaimEntity.builder()
            .languageUsed(LanguageUsed.ENGLISH)
            .genAppExpected(VerticalYesNo.NO)
            .build();
        PcsCaseEntity pcsCase = caseWith(claim);

        assertThat(underTest.shouldSkipClaimPack(pcsCase, claim)).isFalse();
    }

    @Test
    @DisplayName("Skips the claim pack when an Active PF0026 case flag is present")
    void shouldSkipClaimPackWhenActiveCaseFlag() {
        enableRelease13();
        ClaimEntity claim = ClaimEntity.builder().languageUsed(LanguageUsed.ENGLISH).build();
        PcsCaseEntity pcsCase = caseWith(claim);
        pcsCase.setCaseFlags(List.of(welshCaseFlag("Active")));

        assertThat(underTest.shouldSkipClaimPack(pcsCase, claim)).isTrue();
    }

    @Test
    @DisplayName("Skips the claim pack when an Active PF0026 party flag is present")
    void shouldSkipClaimPackWhenActivePartyFlag() {
        enableRelease13();
        PartyEntity claimant = partyWithFlag(welshPartyFlag("Active"));
        ClaimEntity claim = ClaimEntity.builder()
            .languageUsed(LanguageUsed.ENGLISH)
            .claimParties(List.of(claimParty(claimant, PartyRole.CLAIMANT)))
            .build();
        PcsCaseEntity pcsCase = caseWith(claim);

        assertThat(underTest.shouldSkipClaimPack(pcsCase, claim)).isTrue();
    }

    @Test
    @DisplayName("Does not skip the claim pack for an inactive or missing PF0026 flag")
    void shouldNotSkipClaimPackWhenFlagInactiveOrMissing() {
        enableRelease13();
        PartyEntity claimant = partyWithFlag(welshPartyFlag("Inactive"));
        ClaimEntity claim = ClaimEntity.builder()
            .languageUsed(LanguageUsed.ENGLISH)
            .claimParties(List.of(claimParty(claimant, PartyRole.CLAIMANT)))
            .build();
        PcsCaseEntity pcsCase = caseWith(claim);
        pcsCase.setCaseFlags(List.of(welshCaseFlag("Inactive")));

        assertThat(underTest.shouldSkipClaimPack(pcsCase, claim)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    @DisplayName("Skips the defence pack when a submitted defence requires translation")
    void shouldSkipDefencePackWhenSubmittedDefenceNeedsTranslation(LanguageUsed languageUsed) {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(submittedDefence(languageUsed)))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isTrue();
    }

    @Test
    @DisplayName("Skips the defence pack when an issued counterclaim requires translation")
    void shouldSkipDefencePackWhenIssuedCounterClaimNeedsTranslation() {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(submittedDefence(LanguageUsed.WELSH)))
            .counterClaims(List.of(issuedCounterClaim(LanguageUsed.WELSH, null)))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isTrue();
    }

    @Test
    @DisplayName("Skips the defence pack when an issued counterclaim has an HWF reference")
    void shouldSkipDefencePackWhenIssuedCounterClaimHasHwf() {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(submittedDefence(LanguageUsed.ENGLISH)))
            .counterClaims(List.of(issuedCounterClaim(LanguageUsed.ENGLISH, "HWF-123")))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isTrue();
    }

    @Test
    @DisplayName("Skips the defence pack when translation and issued-counterclaim HWF both apply")
    void shouldSkipDefencePackWhenTranslationAndHwf() {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(submittedDefence(LanguageUsed.WELSH)))
            .counterClaims(List.of(issuedCounterClaim(LanguageUsed.WELSH, "HWF-123")))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isTrue();
    }

    @Test
    @DisplayName("Does not skip an English submitted defence with no issued-counterclaim HWF")
    void shouldNotSkipEnglishDefenceWithoutHwf() {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(submittedDefence(LanguageUsed.ENGLISH)))
            .counterClaims(List.of(issuedCounterClaim(LanguageUsed.ENGLISH, null)))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isFalse();
    }

    @Test
    @DisplayName("Does not skip a draft defence that would need translation once submitted")
    void shouldNotSkipCreatedDefenceNeedingTranslation() {
        enableRelease13();
        DefendantResponseEntity draft = DefendantResponseEntity.builder()
            .status(DefendantResponseStatus.CREATED)
            .languageUsed(LanguageUsed.WELSH)
            .build();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(draft))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isFalse();
    }

    @Test
    @DisplayName("Skips the defence pack when a pending-review counterclaim has an HWF reference")
    void shouldSkipDefencePackWhenPendingReviewCounterClaimHasHwf() {
        enableRelease13();
        CounterClaimEntity pendingReview = CounterClaimEntity.builder()
            .status(CounterClaimState.PENDING_REVIEW)
            .languageUsed(LanguageUsed.ENGLISH)
            .hwfReferenceNumber("HWF-123")
            .build();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(submittedDefence(LanguageUsed.ENGLISH)))
            .counterClaims(List.of(pendingReview))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isTrue();
    }

    @Test
    @DisplayName("Does not skip a payment-pending counterclaim that has an HWF reference")
    void shouldNotSkipPaymentPendingCounterClaimWithHwf() {
        enableRelease13();
        CounterClaimEntity pending = CounterClaimEntity.builder()
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .languageUsed(LanguageUsed.ENGLISH)
            .hwfReferenceNumber("HWF-123")
            .build();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(submittedDefence(LanguageUsed.ENGLISH)))
            .counterClaims(List.of(pending))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Does not treat a blank issued-counterclaim HWF reference as populated")
    void shouldNotSkipBlankIssuedCounterClaimHwf(String hwfReferenceNumber) {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(ClaimEntity.builder().build()))
            .defendantResponses(List.of(submittedDefence(LanguageUsed.ENGLISH)))
            .counterClaims(List.of(issuedCounterClaim(LanguageUsed.ENGLISH, hwfReferenceNumber)))
            .build();

        assertThat(underTest.shouldSkipDefencePack(pcsCase)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    @DisplayName("Skips a gen-app pack when the application requires translation and has no HWF")
    void shouldSkipGenAppPackWhenTranslationRequired(LanguageUsed languageUsed) {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(UUID.randomUUID()).build();
        GenAppEntity genApp = GenAppEntity.builder().languageUsed(languageUsed).build();

        assertThat(underTest.shouldSkipGenAppPack(pcsCase, genApp)).isTrue();
    }

    @Test
    @DisplayName("Skips a gen-app pack when translation is required even if HWF is provided")
    void shouldSkipGenAppPackWhenTranslationAndHwf() {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(UUID.randomUUID()).build();
        GenAppEntity genApp = GenAppEntity.builder()
            .languageUsed(LanguageUsed.WELSH)
            .helpWithFeesEntity(HelpWithFeesEntity.builder().hwfReference("HWF-123").build())
            .build();

        assertThat(underTest.shouldSkipGenAppPack(pcsCase, genApp)).isTrue();
    }

    @Test
    @DisplayName("Does not skip an English gen app that has HWF")
    void shouldNotSkipEnglishGenAppWithHwf() {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(UUID.randomUUID()).build();
        GenAppEntity genApp = GenAppEntity.builder()
            .languageUsed(LanguageUsed.ENGLISH)
            .helpWithFeesEntity(HelpWithFeesEntity.builder().hwfReference("HWF-123").build())
            .build();

        assertThat(underTest.shouldSkipGenAppPack(pcsCase, genApp)).isFalse();
    }

    @Test
    @DisplayName("Skips a gen-app pack when an Active PF0026 flag is present")
    void shouldSkipGenAppPackWhenActiveTranslationFlag() {
        enableRelease13();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseFlags(List.of(welshCaseFlag("Active")))
            .build();
        GenAppEntity genApp = GenAppEntity.builder().languageUsed(LanguageUsed.ENGLISH).build();

        assertThat(underTest.shouldSkipGenAppPack(pcsCase, genApp)).isTrue();
    }

    private void enableRelease13() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
    }

    private PcsCaseEntity caseWith(ClaimEntity claim) {
        return PcsCaseEntity.builder().id(UUID.randomUUID()).claims(List.of(claim)).build();
    }

    private PcsCaseEntity welshCaseWithExpectedGenAppAndHwf() {
        ClaimEntity claim = ClaimEntity.builder()
            .languageUsed(LanguageUsed.WELSH)
            .genAppExpected(VerticalYesNo.YES)
            .build();
        return PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .claims(List.of(claim))
            .caseFlags(List.of(welshCaseFlag("Active")))
            .defendantResponses(List.of(submittedDefence(LanguageUsed.WELSH)))
            .counterClaims(List.of(issuedCounterClaim(LanguageUsed.WELSH, "HWF-123")))
            .build();
    }

    private GenAppEntity welshGenApp() {
        return GenAppEntity.builder().languageUsed(LanguageUsed.WELSH).build();
    }

    private DefendantResponseEntity submittedDefence(LanguageUsed languageUsed) {
        return DefendantResponseEntity.builder()
            .status(DefendantResponseStatus.SUBMITTED)
            .languageUsed(languageUsed)
            .build();
    }

    private CounterClaimEntity issuedCounterClaim(LanguageUsed languageUsed, String hwfReferenceNumber) {
        return CounterClaimEntity.builder()
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .languageUsed(languageUsed)
            .hwfReferenceNumber(hwfReferenceNumber)
            .build();
    }

    private PartyEntity partyWithFlag(CasePartyFlagEntity flag) {
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        party.getDefendantFlags().add(flag);
        return party;
    }

    private ClaimPartyEntity claimParty(PartyEntity party, PartyRole role) {
        return ClaimPartyEntity.builder().party(party).role(role).rank(1).build();
    }

    private CaseFlagEntity welshCaseFlag(String status) {
        CaseFlagEntity flag = new CaseFlagEntity();
        applyWelshFlag(flag, status);
        return flag;
    }

    private CasePartyFlagEntity welshPartyFlag(String status) {
        CasePartyFlagEntity flag = new CasePartyFlagEntity();
        applyWelshFlag(flag, status);
        return flag;
    }

    private void applyWelshFlag(BaseCaseFlag flag, String status) {
        flag.setDefaultStatus(status);
        flag.setFlagRefData(FlagRefDataEntity.builder()
            .flagCode("PF0026")
            .build());
    }
}
