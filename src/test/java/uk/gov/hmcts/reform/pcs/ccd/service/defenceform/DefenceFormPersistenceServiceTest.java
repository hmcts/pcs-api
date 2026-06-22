package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefenceFormPersistenceServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final UUID RESPONSE_ID = UUID.randomUUID();
    private static final String DM_STORE_URL = "https://dm-store/xyz";

    @Mock
    private DefendantResponseRepository defendantResponseRepository;
    @Mock
    private DefenceFormPayloadBuilder payloadBuilder;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private ClaimActivityLogService claimActivityLogService;

    @InjectMocks
    private DefenceFormPersistenceService underTest;

    @Test
    void buildsContextWithPayloadAndDefendantNumberWhenNotAttached() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        DefendantResponseEntity response = responseFor(defendant, 2);
        DefenceFormPayload payload = DefenceFormPayload.builder().build();
        when(defendantResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(response));
        when(payloadBuilder.build(response)).thenReturn(payload);

        Optional<DefenceFormRenderContext> context = underTest.buildContextIfNotAttached(RESPONSE_ID);

        assertThat(context).isPresent();
        assertThat(context.get().payload()).isSameAs(payload);
        assertThat(context.get().defendantNumber()).isEqualTo(2);
    }

    @Test
    void returnsEmptyAndDoesNotBuildWhenAlreadyAttached() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        DefendantResponseEntity response = responseFor(defendant, 1);
        response.setSubmissionDocument(DocumentEntity.builder().build());
        when(defendantResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(response));

        assertThat(underTest.buildContextIfNotAttached(RESPONSE_ID)).isEmpty();
        verifyNoInteractions(payloadBuilder);
    }

    @Test
    void attachStoresTypesLinksDocumentAndLogsSuccess() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        DefendantResponseEntity response = responseFor(defendant, 1);
        DocumentEntity document = DocumentEntity.builder().build();
        PcsCaseEntity pcsCase = response.getPcsCase();
        when(defendantResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(response));
        when(documentImportService.addDocumentToCase(
            pcsCase, DM_STORE_URL, CaseFileCategory.STATEMENTS_OF_CASE)).thenReturn(document);

        underTest.attach(RESPONSE_ID, DM_STORE_URL);

        assertThat(document.getType()).isEqualTo(DocumentType.DEFENDANT_RESPONSE);
        assertThat(response.getSubmissionDocument()).isSameAs(document);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, defendant);
    }

    @Test
    void attachSkipsWhenAlreadyAttached() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        DefendantResponseEntity response = responseFor(defendant, 1);
        response.setSubmissionDocument(DocumentEntity.builder().build());
        when(defendantResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(response));

        underTest.attach(RESPONSE_ID, DM_STORE_URL);

        verify(documentImportService, never()).addDocumentToCase(
            any(PcsCaseEntity.class), anyString(), any(CaseFileCategory.class));
        verifyNoInteractions(claimActivityLogService);
    }

    private DefendantResponseEntity responseFor(PartyEntity defendant, int rank) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        ClaimPartyEntity claimParty = ClaimPartyEntity.builder()
            .party(defendant)
            .role(PartyRole.DEFENDANT)
            .rank(rank)
            .build();
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(new ArrayList<>(List.of(claimParty)))
            .build();
        return DefendantResponseEntity.builder()
            .id(RESPONSE_ID)
            .claim(claim)
            .pcsCase(pcsCase)
            .party(defendant)
            .build();
    }
}
