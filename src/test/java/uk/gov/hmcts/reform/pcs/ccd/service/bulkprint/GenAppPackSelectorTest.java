package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDocumentRef;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HelpWithFeesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseFlagService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppPackSelectorTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final long CASE_REF = 1234567890123456L;

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    @Mock
    private FeatureToggleService featureToggleService;

    @Spy
    private SentPackDocuments sentPackDocuments = new SentPackDocuments(new ObjectMapper());

    private GenAppPackSelector underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppPackSelector(
            claimActivityLogRepository,
            sentPackDocuments,
            new PackSkipRules(featureToggleService, new CaseFlagService(null, null, null, null)));
    }

    private final PartyEntity claimant = party();
    private final PartyEntity defendant = party();
    private final PartyEntity coDefendant = party();
    private final DocumentEntity withNoticePdf = gaDocument();
    private final DocumentEntity secondGaPdf = gaDocument();

    @Test
    @DisplayName("Without-notice issued GAs produce no candidates")
    void shouldReturnNothingForWithoutNoticeApplications() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(withoutNoticeGa(withNoticePdf)), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Defendant CUI with-notice GAs produce one candidate per postal party")
    void shouldProduceOneCandidatePerPartyForDefendantCuiWithNoticeApplications() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant)), claimant, defendant, coDefendant));

        assertThat(result).hasSize(3);
        assertThat(candidateFor(result, claimant).role()).isEqualTo(PartyRole.CLAIMANT);
        assertThat(candidateFor(result, claimant).documents()).containsExactly(withNoticePdf);
        assertThat(candidateFor(result, defendant).role()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(candidateFor(result, defendant).documents()).containsExactly(withNoticePdf);
        assertThat(candidateFor(result, coDefendant).documents()).containsExactly(withNoticePdf);
    }

    @Test
    @DisplayName("Includes the defendant applicant among the postal parties")
    void shouldIncludeTheApplicant() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant)), claimant, defendant));

        assertThat(result).extracting(candidate -> candidate.recipient().getId())
            .containsExactly(claimant.getId(), defendant.getId());
    }

    @Test
    @DisplayName("Excludes a party whose contact preferences explicitly opt out of post")
    void shouldExcludeExplicitPostNo() {
        defendant.setContactPreferences(ContactPreferencesEntity.builder().contactByPost(VerticalYesNo.NO).build());
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant)), claimant, defendant));

        assertThat(result).extracting(candidate -> candidate.recipient().getId())
            .containsExactly(claimant.getId());
    }

    @Test
    @DisplayName("Defaults to post when contact preferences are unset")
    void shouldDefaultToPostWhenPreferencesUnset() {
        claimant.setContactPreferences(null);
        defendant.setContactPreferences(ContactPreferencesEntity.builder().build());
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant)), claimant, defendant));

        assertThat(result).extracting(candidate -> candidate.recipient().getId())
            .containsExactly(claimant.getId(), defendant.getId());
    }

    @Test
    @DisplayName("Defaults to post when contactByPost is null on otherwise present preferences")
    void shouldDefaultToPostWhenContactByPostNull() {
        claimant.setContactPreferences(ContactPreferencesEntity.builder().contactByEmail(VerticalYesNo.YES).build());
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant)), claimant, defendant));

        assertThat(candidateFor(result, claimant).documents()).containsExactly(withNoticePdf);
    }

    @Test
    @DisplayName("Includes a party who has explicitly opted in to post")
    void shouldIncludeExplicitPostYes() {
        claimant.setContactPreferences(ContactPreferencesEntity.builder().contactByPost(VerticalYesNo.YES).build());
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant)), claimant, defendant));

        assertThat(candidateFor(result, claimant).documents()).containsExactly(withNoticePdf);
    }

    @Test
    @DisplayName("Skips a (party, document) pair already covered by PACK_SENT")
    void shouldDedupeAlreadySentDocuments() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            sent(claimant, withNoticePdf)));

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant)), claimant, defendant));

        assertThat(result).extracting(candidate -> candidate.recipient().getId())
            .containsExactly(defendant.getId());
    }

    @Test
    @DisplayName("Returns nothing when every party has already been sent the GA")
    void shouldReturnNothingWhenAllSent() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of(
            sent(claimant, withNoticePdf), sent(defendant, withNoticePdf)));

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant)), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Excludes with-notice GAs where the applicant is the claimant")
    void shouldExcludeClaimantApplicantGenApps() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        GenAppEntity claimantGa = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        claimantGa.setParty(claimant);

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(claimantGa), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Excludes defendant ExUI GAs submitted with a firm name on the statement of truth")
    void shouldExcludeDefendantExUiGenApps() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        GenAppEntity exUiGa = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        exUiGa.setStatementOfTruth(StatementOfTruthEntity.builder().firmName("Example Firm").build());

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(exUiGa), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Excludes caseworker-entered GAs with an application received date")
    void shouldExcludeCaseworkerEnteredGenApps() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());

        GenAppEntity caseworkerGa = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        caseworkerGa.setApplicationReceivedDate(LocalDate.now());

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(caseworkerGa), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Multiple defendant CUI with-notice GAs produce a separate candidate per party and GA")
    void shouldProduceSeparateCandidatesPerPartyAndGa() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity first = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        first.setRank(1);
        GenAppEntity second = defendantCuiWithNoticeGa(secondGaPdf, defendant);
        second.setRank(2);

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(first, second), claimant, defendant));

        assertThat(result).hasSize(4);
        assertThat(candidatesFor(result, claimant)).extracting(GenAppPackCandidate::documents)
            .containsExactly(List.of(withNoticePdf), List.of(secondGaPdf));
        assertThat(candidatesFor(result, defendant)).extracting(GenAppPackCandidate::documents)
            .containsExactly(List.of(withNoticePdf), List.of(secondGaPdf));
    }

    @Test
    @DisplayName("Does not pack supporting evidence, only the submission PDF")
    void shouldNotIncludeSupportingEvidence() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        DocumentEntity evidence = DocumentEntity.builder().id(UUID.randomUUID()).type(DocumentType.OTHER).build();
        GenAppEntity genApp = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        genApp.setDocuments(List.of(evidence));

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(genApp), claimant, defendant));

        assertThat(candidateFor(result, claimant).documents()).containsExactly(withNoticePdf);
    }

    @Test
    @DisplayName("Skips GAs that are not yet issued even when a PDF exists")
    void shouldSkipPendingGenApps() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity pending = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        pending.setState(GenAppState.PENDING_GEN_APP_ISSUED);

        assertThat(underTest.findGenAppPackCandidates(caseWith(List.of(pending), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Skips issued with-notice GAs that have no submission PDF")
    void shouldSkipIssuedGenAppsWithoutPdf() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity noPdf = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        noPdf.setSubmissionDocument(null);

        assertThat(underTest.findGenAppPackCandidates(caseWith(List.of(noPdf), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Returns nothing when there is no claim")
    void shouldReturnNothingWhenNoClaim() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(CASE_ID).caseReference(CASE_REF).claims(List.of()).build();

        assertThat(underTest.findGenAppPackCandidates(pcsCase)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    @DisplayName("Does not send a gen-app pack when the application requires translation")
    void shouldSkipGenAppPackWhenTranslationRequired(LanguageUsed languageUsed) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity genApp = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        genApp.setLanguageUsed(languageUsed);

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(genApp), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Does not send a gen-app pack when translation is required even if HWF is provided")
    void shouldSkipGenAppPackWhenTranslationAndHwf() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity genApp = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        genApp.setLanguageUsed(LanguageUsed.WELSH);
        genApp.setHelpWithFeesEntity(HelpWithFeesEntity.builder().hwfReference("HWF-123").build());

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(genApp), claimant, defendant))).isEmpty();
    }

    @Test
    @DisplayName("Still sends an English with-notice gen app that has HWF")
    void shouldStillSendEnglishGenAppWithHwf() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity genApp = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        genApp.setLanguageUsed(LanguageUsed.ENGLISH);
        genApp.setHelpWithFeesEntity(HelpWithFeesEntity.builder().hwfReference("HWF-123").build());

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(genApp), claimant, defendant))).isNotEmpty();
    }

    @Test
    @DisplayName("Sends a Welsh gen app when the rollout flag is off")
    void shouldSendWelshGenAppWhenRolloutFlagOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(false);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity genApp = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        genApp.setLanguageUsed(LanguageUsed.WELSH);

        assertThat(underTest.findGenAppPackCandidates(caseWith(
            List.of(genApp), claimant, defendant))).isNotEmpty();
    }

    @Test
    @DisplayName("Skips only the gen app that requires translation when another English GA is ready")
    void shouldSkipOnlyTheGenAppThatNeedsTranslation() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(true);
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity welshGa = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        welshGa.setRank(1);
        welshGa.setLanguageUsed(LanguageUsed.WELSH);
        GenAppEntity englishGa = defendantCuiWithNoticeGa(secondGaPdf, defendant);
        englishGa.setRank(2);
        englishGa.setLanguageUsed(LanguageUsed.ENGLISH);

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(caseWith(
            List.of(welshGa, englishGa), claimant, defendant));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(GenAppPackCandidate::documents)
            .containsExactly(List.of(secondGaPdf), List.of(secondGaPdf));
    }

    @Test
    @DisplayName("Returns nothing when the claim has no parties")
    void shouldReturnNothingWhenClaimHasNoParties() {
        ClaimEntity claim = ClaimEntity.builder().claimParties(List.of()).build();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(CASE_ID)
            .caseReference(CASE_REF)
            .claims(List.of(claim))
            .genApps(new LinkedHashSet<>(List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant))))
            .build();

        assertThat(underTest.findGenAppPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Still selects packs when the claim has defendants but no claimants")
    void shouldSelectWhenClaimHasDefendantsButNoClaimants() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(List.of(
                ClaimPartyEntity.builder().party(defendant).role(PartyRole.DEFENDANT).rank(1).build()))
            .build();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(CASE_ID)
            .caseReference(CASE_REF)
            .claims(List.of(claim))
            .genApps(new LinkedHashSet<>(List.of(defendantCuiWithNoticeGa(withNoticePdf, defendant))))
            .build();

        List<GenAppPackCandidate> result = underTest.findGenAppPackCandidates(pcsCase);

        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.recipient().getId()).isEqualTo(defendant.getId());
            assertThat(candidate.role()).isEqualTo(PartyRole.DEFENDANT);
            assertThat(candidate.documents()).containsExactly(withNoticePdf);
        });
    }

    @Test
    @DisplayName("Returns nothing when genApps is null")
    void shouldReturnNothingWhenGenAppsNull() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(List.of(
                ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
                ClaimPartyEntity.builder().party(defendant).role(PartyRole.DEFENDANT).rank(1).build()))
            .build();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(CASE_ID).caseReference(CASE_REF).claims(List.of(claim)).genApps(null).build();

        assertThat(underTest.findGenAppPackCandidates(pcsCase)).isEmpty();
    }

    @Test
    @DisplayName("Excludes issued with-notice GAs that have no applicant party")
    void shouldExcludeGenAppsWithNullApplicant() {
        when(claimActivityLogRepository.findAllByPcsCase_Id(CASE_ID)).thenReturn(List.of());
        GenAppEntity noApplicant = defendantCuiWithNoticeGa(withNoticePdf, defendant);
        noApplicant.setParty(null);

        assertThat(underTest.findGenAppPackCandidates(caseWith(List.of(noApplicant), claimant, defendant))).isEmpty();
    }

    private GenAppPackCandidate candidateFor(List<GenAppPackCandidate> result, PartyEntity recipient) {
        return candidatesFor(result, recipient).stream().findFirst()
            .orElseThrow(() -> new AssertionError("no candidate for recipient " + recipient.getId()));
    }

    private List<GenAppPackCandidate> candidatesFor(List<GenAppPackCandidate> result, PartyEntity recipient) {
        return result.stream()
            .filter(candidate -> candidate.recipient().getId().equals(recipient.getId()))
            .toList();
    }

    private PcsCaseEntity caseWith(List<GenAppEntity> genApps, PartyEntity claimantParty,
                                   PartyEntity... defendantParties) {
        List<ClaimPartyEntity> claimParties = new ArrayList<>();
        claimParties.add(ClaimPartyEntity.builder().party(claimantParty).role(PartyRole.CLAIMANT).rank(1).build());
        int rank = 1;
        for (PartyEntity defendantParty : defendantParties) {
            claimParties.add(
                ClaimPartyEntity.builder().party(defendantParty).role(PartyRole.DEFENDANT).rank(rank++).build());
        }
        ClaimEntity claim = ClaimEntity.builder().claimParties(claimParties).build();
        Set<GenAppEntity> genAppSet = new LinkedHashSet<>(genApps);
        return PcsCaseEntity.builder()
            .id(CASE_ID).caseReference(CASE_REF).claims(List.of(claim)).genApps(genAppSet).build();
    }

    private GenAppEntity defendantCuiWithNoticeGa(DocumentEntity document, PartyEntity applicantDefendant) {
        return GenAppEntity.builder()
            .id(UUID.randomUUID())
            .state(GenAppState.GEN_APP_ISSUED)
            .withoutNotice(VerticalYesNo.NO)
            .submissionDocument(document)
            .party(applicantDefendant)
            .build();
    }

    private GenAppEntity withoutNoticeGa(DocumentEntity document) {
        return GenAppEntity.builder()
            .id(UUID.randomUUID())
            .state(GenAppState.GEN_APP_ISSUED)
            .withoutNotice(VerticalYesNo.YES)
            .submissionDocument(document)
            .build();
    }

    private ClaimActivityLogEntity sent(PartyEntity party, DocumentEntity document) {
        try {
            String details = new ObjectMapper().writeValueAsString(PackDetails.sent(
                LetterType.GEN_APP_PACK,
                List.of(new PackDocumentRef(document.getId(), document.getType(), null, false)),
                UUID.randomUUID()));
            return ClaimActivityLogEntity.builder()
                .party(party).details(details)
                .activityType(ClaimActivityType.PACK_SENT).status(ClaimActivityStatus.SUCCESS).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private DocumentEntity gaDocument() {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .type(DocumentType.GENERAL_APPLICATION)
            .build();
    }

    private PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
