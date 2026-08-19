package uk.gov.hmcts.reform.pcs.ccd.service.workallocation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationWAServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private CamundaService camundaService;
    @Mock
    private TaskDescriptionService taskDescriptionService;
    @Mock
    private PartyService partyService;

    @InjectMocks
    private TranslationWAService underTest;

    @Test
    void shouldCreateTranslateDefendantSubmittedDocumentTask() {
        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        List<DocumentEntity> documents = List.of(DocumentEntity.builder().fileName("evidence.pdf").build());

        String expectedDescription = "Defendant 1 has uploaded the following documents: evidence.pdf";
        when(taskDescriptionService.createTranslateDefendantDocumentDescription(
            CASE_REFERENCE, mainClaim, party, documents))
            .thenReturn(expectedDescription);

        underTest.createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, documents);

        verify(camundaService).createTask(
            CASE_REFERENCE, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, expectedDescription);
    }

    @Test
    void shouldNotCreateDefendantTaskWhenDocumentsEmpty() {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();

        underTest.createTranslateDefendantSubmittedDocumentTask(pcsCaseEntity, party, List.of());

        verifyNoInteractions(camundaService, taskDescriptionService);
    }

    @Test
    void shouldCreateTranslateClaimantSubmittedDocumentTask() {
        List<DocumentEntity> documents = List.of(DocumentEntity.builder().fileName("claim-form.pdf").build());

        String expectedDescription = "Claimant 1 has uploaded the following documents: claim-form.pdf";
        when(taskDescriptionService.createTranslateClaimantDocumentDescription(CASE_REFERENCE, documents))
            .thenReturn(expectedDescription);

        underTest.createTranslateClaimantSubmittedDocumentTask(CASE_REFERENCE, documents);

        verify(camundaService).createTask(
            CASE_REFERENCE, TaskType.TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT, expectedDescription);
    }

    @Test
    void shouldNotCreateClaimantTaskWhenDocumentsEmpty() {
        underTest.createTranslateClaimantSubmittedDocumentTask(CASE_REFERENCE, List.of());

        verifyNoInteractions(camundaService, taskDescriptionService);
    }

    @Test
    void shouldCreateClaimantTranslationTaskWhenPartyFlagTriggersTranslation() {
        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        DocumentEntity claimDocument = DocumentEntity.builder()
            .fileName("claim-form.pdf")
            .claim(mainClaim)
            .build();
        DocumentEntity removedDocument = DocumentEntity.builder().claim(mainClaim).removed(true).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .documents(List.of(claimDocument, removedDocument))
            .build();
        PartyEntity flaggingParty = PartyEntity.builder().id(UUID.randomUUID()).pcsCase(pcsCaseEntity).build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(flaggingParty)));

        String expectedDescription = "Claimant 1 has uploaded the following documents: claim-form.pdf";
        when(taskDescriptionService.createTranslateClaimantDocumentDescription(
            CASE_REFERENCE, List.of(claimDocument)))
            .thenReturn(expectedDescription);

        underTest.triggerTranslationTasksForFlaggingParty(flaggingParty);

        verify(camundaService).createTask(
            CASE_REFERENCE, TaskType.TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT, expectedDescription);
    }

    @Test
    void shouldNotCreateClaimantTaskWhenPartyFlagTriggersButNoClaimDocumentsExist() {
        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();
        PartyEntity flaggingParty = PartyEntity.builder().id(UUID.randomUUID()).pcsCase(pcsCaseEntity).build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(flaggingParty)));

        underTest.triggerTranslationTasksForFlaggingParty(flaggingParty);

        verifyNoInteractions(camundaService, taskDescriptionService);
    }

    @Test
    void shouldCreateDefendantTranslationTasksForOtherPartiesWhenPartyFlagTriggersTranslation() {
        UUID flaggingPartyId = UUID.randomUUID();
        UUID otherDefendant1Id = UUID.randomUUID();
        UUID otherDefendant2Id = UUID.randomUUID();
        UUID claimantId = UUID.randomUUID();

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();

        PartyEntity flaggingParty = PartyEntity.builder().id(flaggingPartyId).pcsCase(pcsCaseEntity).build();
        PartyEntity otherDefendant1 = PartyEntity.builder().id(otherDefendant1Id).pcsCase(pcsCaseEntity).build();
        PartyEntity otherDefendant2 = PartyEntity.builder().id(otherDefendant2Id).pcsCase(pcsCaseEntity).build();
        PartyEntity claimant = PartyEntity.builder().id(claimantId).pcsCase(pcsCaseEntity).build();
        pcsCaseEntity.setParties(new HashSet<>(
            List.of(flaggingParty, otherDefendant1, otherDefendant2, claimant)));

        DefendantResponseEntity otherDefendant1Response = DefendantResponseEntity.builder()
            .party(otherDefendant1)
            .build();
        DocumentEntity otherDefendant1Document = DocumentEntity.builder()
            .fileName("defendant1-response.pdf")
            .party(otherDefendant1)
            .defendantResponse(otherDefendant1Response)
            .build();
        DocumentEntity otherDefendant1RemovedDocument = DocumentEntity.builder()
            .party(otherDefendant1)
            .defendantResponse(otherDefendant1Response)
            .removed(true)
            .build();

        GenAppEntity otherDefendant1GenApp = GenAppEntity.builder()
            .party(otherDefendant1)
            .build();
        DocumentEntity otherDefendant1GenAppDocument = DocumentEntity.builder()
            .fileName("defendant1-genapp.pdf")
            .generalApplication(otherDefendant1GenApp)
            .build();

        CounterClaimEntity otherDefendant2CounterClaim = CounterClaimEntity.builder()
            .party(otherDefendant2)
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .build();
        DocumentEntity otherDefendant2Document = DocumentEntity.builder()
            .fileName("defendant2-counterclaim.pdf")
            .party(otherDefendant2)
            .counterClaim(otherDefendant2CounterClaim)
            .build();

        DocumentEntity claimantDocument = DocumentEntity.builder()
            .fileName("claimant-document.pdf")
            .party(claimant)
            .build();

        pcsCaseEntity.setDocuments(
            List.of(otherDefendant1Document, otherDefendant1RemovedDocument, otherDefendant1GenAppDocument,
                otherDefendant2Document, claimantDocument));

        when(partyService.getPartyRole(otherDefendant1)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.getPartyRole(otherDefendant2)).thenReturn(PartyRole.DEFENDANT);
        when(partyService.getPartyRole(claimant)).thenReturn(PartyRole.CLAIMANT);

        String expectedDefendant1Description = "Defendant 1 has uploaded the following documents";
        when(taskDescriptionService.createTranslateDefendantDocumentDescription(
            CASE_REFERENCE, mainClaim, otherDefendant1,
            List.of(otherDefendant1Document, otherDefendant1GenAppDocument)))
            .thenReturn(expectedDefendant1Description);

        String expectedDefendant2Description = "Defendant 2 has uploaded the following documents";
        when(taskDescriptionService.createTranslateDefendantDocumentDescription(
            CASE_REFERENCE, mainClaim, otherDefendant2, List.of(otherDefendant2Document)))
            .thenReturn(expectedDefendant2Description);

        underTest.triggerTranslationTasksForFlaggingParty(flaggingParty);

        verify(camundaService).createTask(
            CASE_REFERENCE, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, expectedDefendant1Description);
        verify(camundaService).createTask(
            CASE_REFERENCE, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, expectedDefendant2Description);
    }

    @Test
    void shouldExcludeAccessCodeAndCounterclaimFormDocumentsFromDefendantTranslationTask() {
        UUID flaggingPartyId = UUID.randomUUID();
        UUID otherDefendantId = UUID.randomUUID();

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();

        PartyEntity flaggingParty = PartyEntity.builder().id(flaggingPartyId).pcsCase(pcsCaseEntity).build();
        PartyEntity otherDefendant = PartyEntity.builder().id(otherDefendantId).pcsCase(pcsCaseEntity).build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(flaggingParty, otherDefendant)));

        DocumentEntity accessCodeDocument = DocumentEntity.builder()
            .fileName("access-code.pdf")
            .party(otherDefendant)
            .type(DocumentType.DEFENDANT_ACCESS_CODE)
            .build();
        DocumentEntity counterClaimFormDocument = DocumentEntity.builder()
            .fileName("counterclaim-form.pdf")
            .party(otherDefendant)
            .type(DocumentType.COUNTERCLAIM)
            .build();
        DocumentEntity evidenceDocument = DocumentEntity.builder()
            .fileName("evidence.pdf")
            .party(otherDefendant)
            .build();
        pcsCaseEntity.setDocuments(List.of(accessCodeDocument, counterClaimFormDocument, evidenceDocument));

        when(partyService.getPartyRole(otherDefendant)).thenReturn(PartyRole.DEFENDANT);

        String expectedDescription = "Defendant 1 has uploaded the following documents";
        when(taskDescriptionService.createTranslateDefendantDocumentDescription(
            CASE_REFERENCE, mainClaim, otherDefendant, List.of(evidenceDocument)))
            .thenReturn(expectedDescription);

        underTest.triggerTranslationTasksForFlaggingParty(flaggingParty);

        verify(camundaService).createTask(
            CASE_REFERENCE, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, expectedDescription);
    }

    @Test
    void shouldExcludeGenAppSubmissionDocumentFromDefendantTranslationTask() {
        UUID flaggingPartyId = UUID.randomUUID();
        UUID otherDefendantId = UUID.randomUUID();

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();

        PartyEntity flaggingParty = PartyEntity.builder().id(flaggingPartyId).pcsCase(pcsCaseEntity).build();
        PartyEntity otherDefendant = PartyEntity.builder().id(otherDefendantId).pcsCase(pcsCaseEntity).build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(flaggingParty, otherDefendant)));

        GenAppEntity otherDefendantGenApp = GenAppEntity.builder()
            .party(otherDefendant)
            .build();
        DocumentEntity genAppSubmissionDocument = DocumentEntity.builder()
            .fileName("genapp-form.pdf")
            .generalApplication(otherDefendantGenApp)
            .build();
        otherDefendantGenApp.setSubmissionDocument(genAppSubmissionDocument);

        DocumentEntity evidenceDocument = DocumentEntity.builder()
            .fileName("evidence.pdf")
            .party(otherDefendant)
            .build();
        pcsCaseEntity.setDocuments(List.of(genAppSubmissionDocument, evidenceDocument));

        when(partyService.getPartyRole(otherDefendant)).thenReturn(PartyRole.DEFENDANT);

        String expectedDescription = "Defendant 1 has uploaded the following documents";
        when(taskDescriptionService.createTranslateDefendantDocumentDescription(
            CASE_REFERENCE, mainClaim, otherDefendant, List.of(evidenceDocument)))
            .thenReturn(expectedDescription);

        underTest.triggerTranslationTasksForFlaggingParty(flaggingParty);

        verify(camundaService).createTask(
            CASE_REFERENCE, TaskType.TRANSLATE_DEFENDANT_SUBMITTED_DOCUMENT, expectedDescription);
    }

    @Test
    void shouldNotCreateDefendantTaskWhenPartyFlagTriggersButNoDocumentsExist() {
        UUID flaggingPartyId = UUID.randomUUID();
        UUID otherDefendantId = UUID.randomUUID();

        ClaimEntity mainClaim = ClaimEntity.builder().id(UUID.randomUUID()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();

        PartyEntity flaggingParty = PartyEntity.builder().id(flaggingPartyId).pcsCase(pcsCaseEntity).build();
        PartyEntity otherDefendant = PartyEntity.builder().id(otherDefendantId).pcsCase(pcsCaseEntity).build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(flaggingParty, otherDefendant)));

        when(partyService.getPartyRole(otherDefendant)).thenReturn(PartyRole.DEFENDANT);

        underTest.triggerTranslationTasksForFlaggingParty(flaggingParty);

        verifyNoInteractions(camundaService, taskDescriptionService);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldRequireTranslationForWelshLanguages(LanguageUsed languageUsed) {
        assertThat(underTest.isTranslationRequired(languageUsed)).isTrue();
    }

    @Test
    void shouldNotRequireTranslationForOtherLanguages() {
        assertThat(underTest.isTranslationRequired(LanguageUsed.ENGLISH)).isFalse();
    }

    @Test
    void shouldNotRequireTranslationWhenLanguageIsNull() {
        assertThat(underTest.isTranslationRequired(null)).isFalse();
    }
}
