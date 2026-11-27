package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantOfPossessionPersistenceServiceTest {

    private static final UUID ENFORCEMENT_ORDER_ID = UUID.randomUUID();
    private static final String DOCUMENT_URL = "http://dm-store/documents/123";

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private WarrantOfPossessionPayloadBuilder payloadBuilder;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private ClaimActivityLogService claimActivityLogService;
    @Mock
    private PartyService partyService;
    @Mock
    private NotificationService notificationService;

    private WarrantOfPossessionPersistenceService underTest;

    private final PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().build();

    @BeforeEach
    void setUp() {
        underTest = new WarrantOfPossessionPersistenceService(enforcementOrderRepository, payloadBuilder,
            documentImportService, claimActivityLogService, partyService, notificationService);

        lenient().when(partyService.getPrimaryClaimantPartyEntity(pcsCase)).thenReturn(claimant);
        lenient().when(documentImportService.addDocumentToCase(any(PcsCaseEntity.class), any(), any()))
            .thenAnswer(invocation -> DocumentEntity.builder().id(UUID.randomUUID()).build());
    }

    @Test
    void shouldBuildNoPayloadWhenTheApplicationIsNotAWarrant() {
        EnforcementOrderEntity writOrder = enforcementOrder(null);
        writOrder.setWritDetails(WritEntity.builder().build());
        stubEnforcementOrder(writOrder);

        assertThat(underTest.buildRenderContext(ENFORCEMENT_ORDER_ID)).isEmpty();
        verifyNoInteractions(payloadBuilder);
    }

    @Test
    void shouldBuildNoPayloadWhenTheWarrantHasAlreadyBeenAttached() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity());
        enforcementOrder.addDocuments(
            List.of(DocumentEntity.builder().type(DocumentType.WARRANT_OF_POSSESSION).build()));
        stubEnforcementOrder(enforcementOrder);

        assertThat(underTest.buildRenderContext(ENFORCEMENT_ORDER_ID)).isEmpty();
        verifyNoInteractions(payloadBuilder);
    }

    @Test
    void shouldBuildThePayloadWhenTheApplicationOnlyHasOtherDocuments() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity());
        enforcementOrder.addDocuments(
            List.of(DocumentEntity.builder().type(DocumentType.RISK_ASSESSMENT).build()));
        stubEnforcementOrder(enforcementOrder);

        assertThat(underTest.buildRenderContext(ENFORCEMENT_ORDER_ID)).isPresent();
    }

    @Test
    void shouldThrowWhenTheEnforcementApplicationIsNotFound() {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.buildRenderContext(ENFORCEMENT_ORDER_ID))
            .isInstanceOf(EnforcementApplicationNotFoundException.class)
            .hasMessageContaining(ENFORCEMENT_ORDER_ID.toString());
    }

    @Test
    void shouldUseThePayloadFromThePayloadBuilder() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity());
        stubEnforcementOrder(enforcementOrder);
        WarrantOfPossessionPayload payload = WarrantOfPossessionPayload.builder().build();
        when(payloadBuilder.build(enforcementOrder)).thenReturn(payload);

        assertThat(underTest.buildRenderContext(ENFORCEMENT_ORDER_ID).orElseThrow().payload()).isSameAs(payload);
    }

    @Test
    void shouldAttachTheWarrantToTheApplicationInTheEnforcementFolder() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity());
        stubEnforcementOrder(enforcementOrder);

        underTest.attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        verify(documentImportService).addDocumentToCase(pcsCase, DOCUMENT_URL, CaseFileCategory.ENFORCEMENT);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, claimant);
        assertThat(enforcementOrder.getDocuments()).singleElement()
            .satisfies(document -> {
                assertThat(document.getType()).isEqualTo(DocumentType.WARRANT_OF_POSSESSION);
                assertThat(document.getEnfApplication()).isEqualTo(enforcementOrder);
            });
    }

    @Test
    void shouldEmailTheClaimantOnceTheWarrantIsAttached() {
        stubEnforcementOrder(enforcementOrder(new WarrantEntity()));

        underTest.attachDocumentAndQueueEmail(ENFORCEMENT_ORDER_ID, DOCUMENT_URL);

        verify(notificationService).sendWarrantOfPossessionEmail(claimant);
    }

    @Test
    void shouldPassTheApplicationRankForTheWarrantName() {
        EnforcementOrderEntity enforcementOrder = enforcementOrder(new WarrantEntity());
        enforcementOrder.setRank(2);
        stubEnforcementOrder(enforcementOrder);

        assertThat(underTest.buildRenderContext(ENFORCEMENT_ORDER_ID).orElseThrow().rank()).isEqualTo(2);
    }

    @Test
    void shouldDefaultTheRankToOneWhenTheApplicationHasNoRank() {
        stubEnforcementOrder(enforcementOrder(new WarrantEntity()));

        assertThat(underTest.buildRenderContext(ENFORCEMENT_ORDER_ID).orElseThrow().rank()).isEqualTo(1);
    }

    private void stubEnforcementOrder(EnforcementOrderEntity enforcementOrder) {
        when(enforcementOrderRepository.findById(ENFORCEMENT_ORDER_ID)).thenReturn(Optional.of(enforcementOrder));
    }

    private EnforcementOrderEntity enforcementOrder(WarrantEntity warrant) {
        return EnforcementOrderEntity.builder()
            .id(ENFORCEMENT_ORDER_ID)
            .pcsCase(pcsCase)
            .warrantDetails(warrant)
            .documents(new ArrayList<>())
            .build();
    }
}
