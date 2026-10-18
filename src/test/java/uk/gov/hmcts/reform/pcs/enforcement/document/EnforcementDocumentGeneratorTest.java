package uk.gov.hmcts.reform.pcs.enforcement.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.config.MapperConfig;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType;

@ExtendWith(MockitoExtension.class)
class EnforcementDocumentGeneratorTest {

    private static final UUID ENFORCEMENT_ID = UUID.randomUUID();
    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String DOCUMENT_URL = "http://dm-store/documents/" + UUID.randomUUID();
    private static final Clock UK_CLOCK =
        Clock.fixed(Instant.parse("2026-07-08T13:05:00Z"), ZoneId.of("Europe/London"));

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private DocAssemblyService docAssemblyService;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private CaseNameFormatter caseNameFormatter;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private LocationReferenceService locationReferenceService;
    @Mock
    private BailiffAllocationRepository bailiffAllocationRepository;

    private final EnforcementDocumentTemplates templates = new EnforcementDocumentTemplates();
    private EnforcementDocumentGenerator underTest;
    private PcsCaseEntity pcsCase;
    private EnforcementOrderEntity enforcement;
    private EnforcementOrder enforcementOrder;

    @BeforeEach
    void setUp() {
        templates.setTemplates(Map.of(
            "warrant-of-possession", "CV-PCS-ENF-Warrant.docx",
            "notice-of-eviction", "CV-PCS-ENF-N54.docx"));
        underTest = new EnforcementDocumentGenerator(enforcementOrderRepository, templates, docAssemblyService,
                                                     documentImportService, new CaseReferenceFormatter(),
                                                     caseNameFormatter, addressMapper, addressFormatter,
                                                     new MapperConfig().modelMapper(), UK_CLOCK,
                                                     locationReferenceService, bailiffAllocationRepository);

        pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(CASE_REFERENCE);
        ClaimEntity claim = new ClaimEntity();
        claim.setPcsCase(pcsCase);
        claim.addParty(PartyEntity.builder().orgName("Treetops Housing").build(), PartyRole.CLAIMANT);
        claim.addParty(PartyEntity.builder().firstName("Jo").lastName("Bloggs").build(), PartyRole.DEFENDANT);
        enforcementOrder = buildEnforcementOrderWithSpecifiedType(SelectEnforcementType.WARRANT);
        enforcement = EnforcementOrderEntity.builder()
            .id(ENFORCEMENT_ID)
            .claim(claim)
            .enforcementOrder(enforcementOrder)
            .enforcementNumber("WP-00000042")
            .bailiffDate(LocalDateTime.parse("2026-07-20T10:00"))
            .dispenseWithService(false)
            .build();
        lenient().when(enforcementOrderRepository.findById(ENFORCEMENT_ID)).thenReturn(Optional.of(enforcement));
    }

    @Test
    void shouldRenderTheDocumentAndFileItInTheEnforcementFolderOnTheCaseAndTheEnforcement() {
        when(caseNameFormatter.formatCaseName(anyList(), anyList())).thenReturn("Treetops Housing vs Jo Bloggs");
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString())).thenReturn(DOCUMENT_URL);
        DocumentEntity imported = DocumentEntity.builder().url(DOCUMENT_URL).build();
        when(documentImportService.addDocumentToCase(pcsCase, DOCUMENT_URL,
                                                     FeatureCaseFileCategory.ENFORCEMENT.getId()))
            .thenReturn(imported);

        Optional<DocumentEntity> generated =
            underTest.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION);

        ArgumentCaptor<EnforcementDocumentPayload> payload = ArgumentCaptor.forClass(EnforcementDocumentPayload.class);
        verify(docAssemblyService).generateDocument(payload.capture(), eq("CV-PCS-ENF-Warrant.docx"),
                                                    eq(OutputType.PDF), eq("Warrant of possession - WP-00000042"));
        assertThat(payload.getValue().getDocumentTitle()).isEqualTo("Warrant of possession");
        assertThat(payload.getValue().getCaseReference()).isEqualTo("1234-5678-9012-3456");
        assertThat(payload.getValue().getCaseName()).isEqualTo("Treetops Housing vs Jo Bloggs");
        assertThat(payload.getValue().getEnforcementType()).isEqualTo("Warrant of possession");
        assertThat(payload.getValue().getEnforcementNumber()).isEqualTo("WP-00000042");
        assertThat(payload.getValue().getSeal()).isEqualTo("County Court");
        assertThat(payload.getValue().getGeneratedAt()).isEqualTo("08/07/2026 02:05 PM");
        assertThat(payload.getValue().getEnforcementOrder()).isSameAs(enforcementOrder);

        assertThat(generated).containsSame(imported);
        assertThat(imported.getType()).isEqualTo(DocumentType.WARRANT_OF_POSSESSION);
        assertThat(enforcement.getDocuments()).containsExactly(imported);
        verify(enforcementOrderRepository).save(enforcement);
    }

    @Test
    void shouldPassTheEvictionToTheNoticesAndLeaveAnUnnumberedDocumentNamedByCase() {
        enforcement.setEnforcementNumber(null);
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString())).thenReturn(DOCUMENT_URL);
        when(documentImportService.addDocumentToCase(any(PcsCaseEntity.class), anyString(), anyString()))
            .thenReturn(DocumentEntity.builder().build());

        underTest.generate(ENFORCEMENT_ID, EnforcementOutput.NOTICE_OF_EVICTION);

        ArgumentCaptor<EnforcementDocumentPayload> payload = ArgumentCaptor.forClass(EnforcementDocumentPayload.class);
        verify(docAssemblyService).generateDocument(payload.capture(), eq("CV-PCS-ENF-N54.docx"), eq(OutputType.PDF),
                                                    eq("Notice of eviction - 1234-5678-9012-3456"));
        assertThat(payload.getValue().getEvictionDateTime()).isEqualTo(LocalDateTime.parse("2026-07-20T10:00"));
        assertThat(payload.getValue().getDispenseWithService()).isFalse();
    }

    @Test
    void shouldNameTheCaseManagementCourtTheAllocatedBailiffAndTheLanguageUsed() {
        pcsCase.setCaseManagementLocation(20262);
        when(locationReferenceService.getCourtVenues(List.of(20262))).thenReturn(List.of(
            courtVenue("Central London County Court", "Thomas More Building, Royal Courts of Justice, Strand",
                       "WC2A 2LL")));
        when(bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(ENFORCEMENT_ID))
            .thenReturn(List.of(BailiffAllocationEntity.builder().bailiffName("Bailey Bailiff").build()));
        enforcementOrder.getWarrantDetails().setLanguageUsed(LanguageUsed.ENGLISH_AND_WELSH);

        EnforcementDocumentPayload payload =
            underTest.buildPayload(enforcement, EnforcementOutput.NOTICE_OF_APPOINTMENT, pcsCase);

        assertThat(payload.getCourtName()).isEqualTo("Central London County Court");
        assertThat(payload.getCourtAddress())
            .isEqualTo("Thomas More Building\nRoyal Courts of Justice\nStrand\nWC2A 2LL");
        assertThat(payload.getBailiffName()).isEqualTo("Bailey Bailiff");
        assertThat(payload.getLanguage()).isEqualTo("English and Welsh");
    }

    @Test
    void shouldNameTheBailiffByEmailWhenTheAllocationHasNoName() {
        when(bailiffAllocationRepository.findByEnforcementOrderIdAndEndedAtIsNull(ENFORCEMENT_ID))
            .thenReturn(List.of(BailiffAllocationEntity.builder().bailiffEmail("bailiff@justice.gov.uk").build()));

        EnforcementDocumentPayload payload =
            underTest.buildPayload(enforcement, EnforcementOutput.NOTICE_OF_APPOINTMENT, pcsCase);

        assertThat(payload.getBailiffName()).isEqualTo("bailiff@justice.gov.uk");
    }

    @Test
    void shouldLeaveTheCourtBailiffAndLanguageOutWhenThereAreNone() {
        EnforcementDocumentPayload payload =
            underTest.buildPayload(enforcement, EnforcementOutput.WARRANT_OF_POSSESSION, pcsCase);

        assertThat(payload.getCourtName()).isNull();
        assertThat(payload.getCourtAddress()).isNull();
        assertThat(payload.getBailiffName()).isNull();
        assertThat(payload.getLanguage()).isNull();
        verifyNoInteractions(locationReferenceService);
    }

    @Test
    void shouldLeaveTheCourtOutWhenLocationReferenceDataHasNoVenue() {
        pcsCase.setCaseManagementLocation(20262);
        when(locationReferenceService.getCourtVenues(List.of(20262))).thenReturn(List.of());

        EnforcementDocumentPayload payload =
            underTest.buildPayload(enforcement, EnforcementOutput.WARRANT_OF_POSSESSION, pcsCase);

        assertThat(payload.getCourtName()).isNull();
        assertThat(payload.getCourtAddress()).isNull();
    }

    @Test
    void shouldTakeTheLanguageFromTheWritAnswers() {
        enforcement.setEnforcementOrder(buildEnforcementOrderWithSpecifiedType(SelectEnforcementType.WRIT));
        enforcement.getEnforcementOrder().getWritDetails().setLanguageUsed(LanguageUsed.WELSH);

        EnforcementDocumentPayload payload =
            underTest.buildPayload(enforcement, EnforcementOutput.WRIT_OF_POSSESSION, pcsCase);

        assertThat(payload.getLanguage()).isEqualTo("Welsh");
    }

    @Test
    void shouldSendTheClaimantsAnswersToTheTemplateAsJson() throws Exception {
        EnforcementDocumentPayload payload =
            underTest.buildPayload(enforcement, EnforcementOutput.RISK_ASSESSMENT, pcsCase);

        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(payload);

        assertThat(json.path("documentTitle").asText()).isEqualTo("Risk assessment");
        assertThat(json.path("seal").isNull()).isTrue();
        assertThat(json.path("enforcementOrder").path("chooseEnforcementType").path("value").path("code").asText())
            .isEqualTo("WARRANT");
    }

    @Test
    void shouldNotGenerateASecondWarrant() {
        enforcement.addDocuments(List.of(DocumentEntity.builder().type(DocumentType.WARRANT_OF_POSSESSION).build()));

        assertThat(underTest.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION)).isEmpty();

        verifyNoInteractions(docAssemblyService, documentImportService);
    }

    @Test
    void shouldGenerateAnotherNoticeOfEvictionForARescheduledEviction() {
        enforcement.addDocuments(List.of(DocumentEntity.builder().type(DocumentType.NOTICE_OF_EVICTION).build()));
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString())).thenReturn(DOCUMENT_URL);
        when(documentImportService.addDocumentToCase(any(PcsCaseEntity.class), anyString(), anyString()))
            .thenReturn(DocumentEntity.builder().build());

        assertThat(underTest.generate(ENFORCEMENT_ID, EnforcementOutput.NOTICE_OF_EVICTION)).isPresent();
        assertThat(enforcement.getDocuments()).hasSize(2);
    }

    @Test
    void shouldSkipADocumentWithNoTemplateConfigured() {
        assertThat(underTest.generate(ENFORCEMENT_ID, EnforcementOutput.RISK_ASSESSMENT)).isEmpty();

        verifyNoInteractions(docAssemblyService, documentImportService);
        verify(enforcementOrderRepository, never()).save(any());
    }

    @Test
    void shouldLetADocmosisFailureThroughSoTheTaskRetries() {
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString()))
            .thenThrow(new DocAssemblyException("Document generation failed"));

        assertThatThrownBy(() -> underTest.generate(ENFORCEMENT_ID, EnforcementOutput.WARRANT_OF_POSSESSION))
            .isInstanceOf(DocAssemblyException.class);
        verifyNoInteractions(documentImportService);
        verify(enforcementOrderRepository, never()).save(any());
    }

    private static CourtVenue courtVenue(String courtName, String courtAddress, String postcode) {
        return new CourtVenue(
            null, null, null, null, null, null, null, null, null, null,
            courtAddress,
            postcode,
            null, null, null, null, null, null, null, null,
            courtName,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        );
    }
}
