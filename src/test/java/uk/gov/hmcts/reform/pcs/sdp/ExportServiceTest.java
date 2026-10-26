package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.SdpClaimSnapshotEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.SdpClaimSnapshotRepository;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-09T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final String HASH = "current-hash";

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    @Mock
    private SnapshotHasher snapshotHasher;

    @Mock
    private SdpClaimSnapshotRepository sdpClaimSnapshotRepository;

    @Mock
    private BlobUploader blobUploader;

    @Captor
    private ArgumentCaptor<List<SdpClaimSnapshotEntity>> savedCaptor;

    @Captor
    private ArgumentCaptor<byte[]> uploadedJsonCaptor;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = exportService(Optional.of(blobUploader));
    }

    private ExportService exportService(Optional<BlobUploader> uploader) {
        ObjectMapper objectMapper = new JacksonConfiguration().getMapper();
        return new ExportService(
            claimRepository,
            claimActivityLogRepository,
            new SnapshotMapper(),
            new ClaimSnapshotFileWriter(objectMapper),
            snapshotHasher,
            sdpClaimSnapshotRepository,
            uploader,
            FIXED_CLOCK,
            "local"
        );
    }

    @Test
    void shouldExtractClaimsFromDbAndWriteJsonFile() throws Exception {
        givenClaimWithHash();
        when(claimActivityLogRepository.findAllByPcsCase_IdInAndActivityType(
            List.of(CASE_ID), ClaimActivityType.DOCUMENTS_CREATED)).thenReturn(List.of(
                ClaimActivityLogEntity.builder()
                    .pcsCase(PcsCaseEntity.builder().id(CASE_ID).build())
                    .activityType(ClaimActivityType.DOCUMENTS_CREATED)
                    .createdAt(LocalDateTime.of(2026, 2, 5, 10, 0))
                    .build()
            ));

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(result.filename()).isEqualTo("claim_snapshot_2026-03-08.json");

        String json = uploadedJson();
        assertThat(json).contains("\"contract_version\" : \"4.1.0\"");
        assertThat(json).contains("\"claim_reference\" : \"1234567890123456\"");
        assertThat(json).contains("\"claim_type\" : \"AAA3-SLD\"");
        assertThat(json).contains("\"name\" : \"Jane Doe\"");
        assertThat(json).contains("\"environment\" : \"local\"");
        assertThat(json).contains("\"snapshot_version\" : 1");
        assertThat(json).contains("\"published_date\" : \"2026-02-05T10:00:00Z\"");
        verify(blobUploader).upload(eq("claim_snapshot_2026-03-08.json"), any(byte[].class));
    }

    @Test
    void shouldExportNewClaimAsVersionOne() throws Exception {
        givenClaimWithHash();

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(result.filename()).isEqualTo("claim_snapshot_2026-03-08.json");
        assertThat(uploadedJson()).contains("\"snapshot_version\" : 1");
        verify(blobUploader).upload(eq("claim_snapshot_2026-03-08.json"), any(byte[].class));
        verify(sdpClaimSnapshotRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getClaimId()).isEqualTo(CLAIM_ID);
            assertThat(saved.getSnapshotVersion()).isEqualTo(1);
            assertThat(saved.getSnapshotHash()).isEqualTo(HASH);
        });
    }

    @Test
    void shouldIncrementVersionWhenClaimHasChanged() throws Exception {
        givenClaimWithHash();
        givenStoredSnapshot(2, "old-hash");

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"snapshot_version\" : 3");
        verify(sdpClaimSnapshotRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getSnapshotVersion()).isEqualTo(3);
            assertThat(saved.getSnapshotHash()).isEqualTo(HASH);
        });
    }

    @Test
    void shouldKeepUnchangedClaimExportedTodayWhenAnotherClaimChanges() throws Exception {
        UUID unchangedId = UUID.randomUUID();
        ClaimEntity changedClaim = claim();
        ClaimEntity unchangedClaim = claim();
        unchangedClaim.setId(unchangedId);
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of(changedClaim, unchangedClaim));
        when(snapshotHasher.hash(any())).thenReturn(HASH);
        when(sdpClaimSnapshotRepository.findAllById(anyList())).thenReturn(List.of(
            SdpClaimSnapshotEntity.builder()
                .claimId(CLAIM_ID)
                .snapshotVersion(1)
                .snapshotHash("old-hash")
                .exportedAt(LocalDateTime.of(2026, 3, 9, 2, 0))
                .build(),
            SdpClaimSnapshotEntity.builder()
                .claimId(unchangedId)
                .snapshotVersion(1)
                .snapshotHash(HASH)
                .exportedAt(LocalDateTime.of(2026, 3, 9, 2, 0))
                .build()
        ));

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(2);
        String json = uploadedJson();
        assertThat(json).contains("\"snapshot_version\" : 2");
        assertThat(json).contains("\"snapshot_version\" : 1");
        verify(sdpClaimSnapshotRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getClaimId()).isEqualTo(CLAIM_ID);
            assertThat(saved.getSnapshotVersion()).isEqualTo(2);
        });
    }

    @Test
    void shouldSkipUnchangedClaimAndNotUpload() {
        givenClaimWithHash();
        givenStoredSnapshot(2, HASH);

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isZero();
        verifyNoInteractions(blobUploader);
        verify(sdpClaimSnapshotRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldNotSaveSnapshotsWhenUploadFails() {
        givenClaimWithHash();
        doThrow(new IllegalStateException("upload failed"))
            .when(blobUploader).upload(any(), any(byte[].class));

        assertThatThrownBy(() -> exportService.exportClaims())
            .isInstanceOf(IllegalStateException.class);
        verify(sdpClaimSnapshotRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldSkipUploadWhenBlobUploaderIsNotAvailable() {
        exportService = exportService(Optional.empty());
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of());

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isZero();
        assertThat(result.filename()).isEqualTo("claim_snapshot_2026-03-08.json");
        verifyNoInteractions(blobUploader);
    }

    @Test
    void shouldExportClaimWhenTenancyLicenceIsMissing() throws Exception {
        ClaimEntity claimWithoutTenancy = claim();
        claimWithoutTenancy.getPcsCase().setTenancyLicence(null);
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of(claimWithoutTenancy));
        when(snapshotHasher.hash(any())).thenReturn(HASH);

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"tenancy_type\" : \"AAA3-OTH\"");
    }

    @Test
    void shouldExportClaimWhenNoticeOfPossessionIsMissing() throws Exception {
        ClaimEntity claimWithoutNotice = claim();
        claimWithoutNotice.setNoticeOfPossession(null);
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of(claimWithoutNotice));
        when(snapshotHasher.hash(any())).thenReturn(HASH);

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).doesNotContain("postal_claim_received_by_court_date");
    }

    @Test
    void shouldExportClaimWhenPartiesAreMissing() throws Exception {
        ClaimEntity claimWithoutParties = claim();
        claimWithoutParties.setClaimParties(null);
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of(claimWithoutParties));
        when(snapshotHasher.hash(any())).thenReturn(HASH);

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"parties\" : [ ]");
    }

    @Test
    void shouldExportClaimWhenPartiesAreEmpty() throws Exception {
        ClaimEntity claimWithEmptyParties = claim();
        claimWithEmptyParties.setClaimParties(List.of());
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of(claimWithEmptyParties));
        when(snapshotHasher.hash(any())).thenReturn(HASH);

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"parties\" : [ ]");
    }

    @Test
    void shouldSkipClaimPartyWhenPartyIsMissing() throws Exception {
        ClaimEntity claimWithNullParty = claim();
        claimWithNullParty.setClaimParties(List.of(
            ClaimPartyEntity.builder().role(PartyRole.DEFENDANT).build()
        ));
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of(claimWithNullParty));
        when(snapshotHasher.hash(any())).thenReturn(HASH);

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"parties\" : [ ]");
    }

    private String uploadedJson() {
        verify(blobUploader).upload(any(), uploadedJsonCaptor.capture());
        return new String(uploadedJsonCaptor.getValue(), StandardCharsets.UTF_8);
    }

    private void givenClaimWithHash() {
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of(claim()));
        when(snapshotHasher.hash(any())).thenReturn(HASH);
    }

    private void givenStoredSnapshot(int version, String hash) {
        when(sdpClaimSnapshotRepository.findAllById(List.of(CLAIM_ID))).thenReturn(List.of(
            SdpClaimSnapshotEntity.builder()
                .claimId(CLAIM_ID)
                .snapshotVersion(version)
                .snapshotHash(hash)
                .exportedAt(LocalDateTime.of(2026, 3, 1, 2, 0))
                .build()
        ));
    }

    private static ClaimEntity claim() {
        PartyEntity party = PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Doe")
            .emailAddress("jane@example.com")
            .build();

        ClaimPartyEntity claimParty = ClaimPartyEntity.builder()
            .party(party)
            .role(PartyRole.DEFENDANT)
            .build();

        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .servingMethod(NoticeServiceMethod.FIRST_CLASS_POST)
            .noticeDate(LocalDate.of(2026, 1, 10))
            .build();

        TenancyLicenceEntity tenancy = TenancyLicenceEntity.builder()
            .type(CombinedLicenceType.SECURE_TENANCY)
            .build();

        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(CASE_ID)
            .caseReference(1234567890123456L)
            .caseManagementLocation(45)
            .tenancyLicence(tenancy)
            .build();

        return ClaimEntity.builder()
            .id(CLAIM_ID)
            .pcsCase(pcsCase)
            .claimantType(ClaimantType.PROVIDER_OF_SOCIAL_HOUSING)
            .againstTrespassers(VerticalYesNo.NO)
            .languageUsed(LanguageUsed.ENGLISH)
            .claimSubmittedDate(LocalDateTime.of(2026, 2, 1, 14, 30))
            .claimIssuedDate(LocalDateTime.of(2026, 2, 5, 9, 0))
            .noticeOfPossession(notice)
            .claimParties(List.of(claimParty))
            .build();
    }
}
