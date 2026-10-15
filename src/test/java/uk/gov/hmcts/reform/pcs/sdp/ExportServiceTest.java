package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-09T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @TempDir
    private Path tempDir;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    @Mock
    private BlobUploader blobUploader;

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
            uploader,
            FIXED_CLOCK,
            "local",
            tempDir.toString()
        );
    }

    @Test
    void shouldExtractClaimsFromDbAndWriteJsonFile() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        PartyEntity party = PartyEntity.builder()
            .id(partyId)
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
            .id(caseId)
            .caseReference(1234567890123456L)
            .caseManagementLocation(45)
            .tenancyLicence(tenancy)
            .build();

        ClaimEntity claim = ClaimEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .claimantType(ClaimantType.PROVIDER_OF_SOCIAL_HOUSING)
            .againstTrespassers(VerticalYesNo.NO)
            .languageUsed(LanguageUsed.ENGLISH)
            .claimSubmittedDate(LocalDateTime.of(2026, 2, 1, 14, 30))
            .claimIssuedDate(LocalDateTime.of(2026, 2, 5, 9, 0))
            .noticeOfPossession(notice)
            .claimParties(List.of(claimParty))
            .build();

        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of(claim));
        when(claimActivityLogRepository.findAllByPcsCase_Id(caseId)).thenReturn(List.of(
            ClaimActivityLogEntity.builder()
                .activityType(ClaimActivityType.DOCUMENTS_CREATED)
                .createdAt(LocalDateTime.of(2026, 2, 5, 10, 0))
                .build()
        ));

        ExportResult result = exportService.exportAllClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(result.outputPath()).exists();
        assertThat(result.outputPath().getFileName().toString())
            .isEqualTo("claim_snapshot_full_2026-03-09.json");

        String json = Files.readString(result.outputPath());
        assertThat(json).contains("\"contract_version\" : \"4.1.0\"");
        assertThat(json).contains("\"claim_reference\" : \"1234567890123456\"");
        assertThat(json).contains("\"claim_type\" : \"AAA3-SLD\"");
        assertThat(json).contains("\"name\" : \"Jane Doe\"");
        assertThat(json).contains("\"environment\" : \"local\"");
        verify(blobUploader).upload(eq("claim_snapshot_full_2026-03-09.json"), any(byte[].class));
    }

    @Test
    void shouldExportClaimsSubmittedOnThePreviousDay() {
        LocalDateTime from = LocalDateTime.of(2026, 3, 8, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 3, 9, 0, 0);
        when(claimRepository.findForSdpSnapshotExportBetween(from, to)).thenReturn(List.of());

        ExportResult result = exportService.exportDeltaClaims();

        assertThat(result.recordCount()).isZero();
        assertThat(result.outputPath().getFileName().toString())
            .isEqualTo("claim_snapshot_delta_2026-03-08.json");
        verify(blobUploader).upload(eq("claim_snapshot_delta_2026-03-08.json"), any(byte[].class));
    }

    @Test
    void shouldSkipUploadWhenBlobUploaderIsNotAvailable() {
        exportService = exportService(Optional.empty());
        when(claimRepository.findAllForSdpSnapshotExport()).thenReturn(List.of());

        ExportResult result = exportService.exportAllClaims();

        assertThat(result.recordCount()).isZero();
        assertThat(result.outputPath()).exists();
        verifyNoInteractions(blobUploader);
    }
}
