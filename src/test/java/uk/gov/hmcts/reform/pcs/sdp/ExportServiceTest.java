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
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.SdpClaimSnapshotEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.sdp.ExportService.EXPORT_CHUNK_SIZE;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-09T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();

    @Mock
    private SdpClaimSnapshotJdbcRepository sdpClaimSnapshotJdbcRepository;

    @Mock
    private BlobUploader blobUploader;

    @Captor
    private ArgumentCaptor<List<SdpClaimSnapshotEntity>> savedCaptor;

    private String uploadedJsonContent;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            uploadedJsonContent = Files.readString(invocation.getArgument(1));
            return null;
        }).when(blobUploader).upload(any(), any(Path.class));
        exportService = exportService(Optional.of(blobUploader));
    }

    private ExportService exportService(Optional<BlobUploader> uploader) {
        return exportService(uploader, "local");
    }

    private ExportService exportService(Optional<BlobUploader> uploader, String environment) {
        ObjectMapper objectMapper = new JacksonConfiguration().getMapper();
        return new ExportService(
            sdpClaimSnapshotJdbcRepository,
            new SnapshotMapper(),
            new ClaimSnapshotFileWriter(objectMapper),
            uploader,
            objectMapper,
            FIXED_CLOCK,
            environment
        );
    }

    @Test
    void shouldExtractClaimsFromDbAndWriteJsonFile() {
        stubDefaultClaim();

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(result.filename()).isEqualTo("claim_snapshot_2026-03-08.json");

        String json = uploadedJson();
        assertThat(json).contains(
            "\"contract_version\":\"4.2.0\"",
            "\"claim_reference\":\"1234567890123456\"",
            "\"claim_type\":\"AAA3-SLD\"",
            "\"claim_sub_type\":\"Social\"",
            "\"party_role\":\"Defendant\"",
            "\"party_represented\":\"No\"",
            "\"party_language\":\"eng\"",
            "\"grounds\":[]",
            "\"property_address\":[]",
            "\"requested_fees\":[]",
            "\"responses\":[]",
            "\"postal_claim_received_by_court_date\":\"2026-01-10\"",
            "\"claim_submitted_to_system_date\":\"2026-02-01T14:30:00Z\"",
            "\"claim_fee_paid_date\":null",
            "\"claim_issued_to_defendant_date\":\"2026-02-05T09:00:00Z\"",
            "\"recorded_date\":\"2026-02-01T14:30:00Z\"",
            "\"environment\":\"dev\"",
            "\"snapshot_version\":1",
            "\"published_date\":\"2026-03-09T12:00:00Z\""
        );
        verify(blobUploader).upload(eq("claim_snapshot_2026-03-08.json"), any(Path.class));
    }

    @Test
    void shouldUsePreviewFilenameWhenExportingFromPreview() {
        stubDefaultClaim();
        exportService = exportService(Optional.of(blobUploader), "preview");

        ExportResult result = exportService.exportClaims();

        assertThat(result.filename()).isEqualTo("claim_snapshot_preview_2026-03-08.json");
        verify(blobUploader).upload(eq("claim_snapshot_preview_2026-03-08.json"), any(Path.class));
    }

    @Test
    void shouldExportNewClaimAsVersionOne() {
        stubDefaultClaim();

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(result.filename()).isEqualTo("claim_snapshot_2026-03-08.json");
        assertThat(uploadedJson()).contains("\"snapshot_version\":1");
        verify(blobUploader).upload(eq("claim_snapshot_2026-03-08.json"), any(Path.class));
        verify(sdpClaimSnapshotJdbcRepository).upsertSnapshots(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getClaimId()).isEqualTo(CLAIM_ID);
            assertThat(saved.getSnapshotVersion()).isEqualTo(1);
        });
    }

    @Test
    void shouldIncrementVersionWhenClaimWasPreviouslyExported() {
        stubClaimsForExport(
            List.of(header(CLAIM_ID, 2)),
            List.of(new SdpClaimSnapshotParty(CLAIM_ID, PARTY_ID, PartyRole.DEFENDANT.name(), false)),
            List.of(),
            List.of(),
            List.of()
        );

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"snapshot_version\":3");
        verify(sdpClaimSnapshotJdbcRepository).upsertSnapshots(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getSnapshotVersion()).isEqualTo(3);
        });
    }

    @Test
    void shouldExportAllSelectedClaimsAndIncrementEachVersion() {
        UUID secondId = UUID.randomUUID();
        stubClaimsForExport(
            List.of(header(CLAIM_ID, 1), header(secondId, 1)),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(2);
        assertThat(uploadedJson()).contains("\"snapshot_version\":2");
        verify(sdpClaimSnapshotJdbcRepository).upsertSnapshots(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(2)
            .allSatisfy(saved -> assertThat(saved.getSnapshotVersion()).isEqualTo(2));
    }

    @Test
    void shouldFailWhenUploadFails() {
        stubDefaultClaim();
        doThrow(new IllegalStateException("upload failed"))
            .when(blobUploader).upload(any(), any(Path.class));

        assertThatThrownBy(() -> exportService.exportClaims())
            .isInstanceOf(IllegalStateException.class);
        verify(sdpClaimSnapshotJdbcRepository, never()).upsertSnapshots(anyList());
    }

    @Test
    void shouldSkipUploadWhenBlobUploaderIsNotAvailable() {
        exportService = exportService(Optional.empty());
        stubClaimsForExport(List.of(), List.of(), List.of(), List.of(), List.of());

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isZero();
        assertThat(result.filename()).isEqualTo("claim_snapshot_2026-03-08.json");
        verifyNoInteractions(blobUploader);
    }

    @Test
    void shouldExportPropertyAddressFromCase() {
        SdpClaimSnapshotHeader headerWithAddress = new SdpClaimSnapshotHeader(
            CLAIM_ID,
            ClaimantType.PROVIDER_OF_SOCIAL_HOUSING.name(),
            VerticalYesNo.NO.name(),
            LanguageUsed.ENGLISH.name(),
            LocalDateTime.of(2026, 2, 1, 14, 30),
            LocalDateTime.of(2026, 2, 5, 9, 0),
            1234567890123456L,
            45,
            null,
            CombinedLicenceType.SECURE_TENANCY.name(),
            NoticeServiceMethod.FIRST_CLASS_POST.name(),
            LocalDate.of(2026, 1, 10),
            null,
            "1 High Street",
            null,
            null,
            "Cambridge",
            null,
            "England",
            "cb5 8bl"
        );
        stubClaimsForExport(List.of(headerWithAddress), List.of(), List.of(), List.of(), List.of());

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        String json = uploadedJson();
        assertThat(json).contains(
            "\"address\":\"1 High Street, Cambridge, England\"",
            "\"postcode\":\"CB5 8BL\""
        );
    }

    @Test
    void shouldExportClaimWhenTenancyLicenceIsMissing() {
        SdpClaimSnapshotHeader headerWithoutTenancy = new SdpClaimSnapshotHeader(
            CLAIM_ID,
            ClaimantType.PROVIDER_OF_SOCIAL_HOUSING.name(),
            VerticalYesNo.NO.name(),
            LanguageUsed.ENGLISH.name(),
            LocalDateTime.of(2026, 2, 1, 14, 30),
            LocalDateTime.of(2026, 2, 5, 9, 0),
            1234567890123456L,
            45,
            null,
            null,
            NoticeServiceMethod.FIRST_CLASS_POST.name(),
            LocalDate.of(2026, 1, 10),
            null,
            null, null, null, null, null, null, null
        );
        stubClaimsForExport(List.of(headerWithoutTenancy), List.of(), List.of(), List.of(), List.of());

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"tenancy_type\":\"AAA3-OTH\"");
    }

    @Test
    void shouldExportClaimWhenNoticeOfPossessionIsMissing() {
        SdpClaimSnapshotHeader headerWithoutNotice = new SdpClaimSnapshotHeader(
            CLAIM_ID,
            ClaimantType.PROVIDER_OF_SOCIAL_HOUSING.name(),
            VerticalYesNo.NO.name(),
            LanguageUsed.ENGLISH.name(),
            LocalDateTime.of(2026, 2, 1, 14, 30),
            LocalDateTime.of(2026, 2, 5, 9, 0),
            1234567890123456L,
            45,
            null,
            CombinedLicenceType.SECURE_TENANCY.name(),
            null,
            null,
            null,
            null, null, null, null, null, null, null
        );
        stubClaimsForExport(List.of(headerWithoutNotice), List.of(), List.of(), List.of(), List.of());

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"postal_claim_received_by_court_date\":null");
    }

    @Test
    void shouldExportClaimWhenPartiesAreMissing() {
        stubClaimsForExport(List.of(header()), List.of(), List.of(), List.of(), List.of());

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"parties\":[]");
    }

    @Test
    void shouldExportGroundsPaymentsResponsesAndRepresentation() {
        stubClaimsForExport(
            List.of(header()),
            List.of(new SdpClaimSnapshotParty(CLAIM_ID, PARTY_ID, PartyRole.DEFENDANT.name(), true)),
            List.of(
                new SdpClaimSnapshotPayment(
                    CLAIM_ID,
                    10,
                    PARTY_ID,
                    "2026-123456",
                    "RC-1234",
                    new BigDecimal("355.00"),
                    PaymentStatus.PAID.name(),
                    "CLAIM",
                    LocalDateTime.of(2026, 2, 3, 10, 0)
                ),
                new SdpClaimSnapshotPayment(
                    CLAIM_ID, 11, PARTY_ID, null, " ", null, PaymentStatus.NOT_PAID.name(), "CLAIM", null)
            ),
            List.of(new SdpClaimSnapshotGround(
                CLAIM_ID,
                "SERIOUS_RENT_ARREARS_GROUND8",
                "Serious rent arrears",
                ClaimGroundCategory.ASSURED_MANDATORY.name()
            )),
            List.of(
                new SdpClaimSnapshotResponse(
                    CLAIM_ID, 12, PARTY_ID, VerticalYesNo.NO.name(), LocalDateTime.of(2026, 3, 1, 10, 0)),
                new SdpClaimSnapshotResponse(UUID.randomUUID(), 13, null, null, null)
            )
        );

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        String json = uploadedJson();
        assertThat(json).contains("\"grounds_code\":\"SERIOUS_RENT_ARREARS_GROUND8\"");
        assertThat(json).contains("\"fee_reference\":\"2026-123456\"");
        assertThat(json).contains("\"fee_status\":\"Fully Paid\"");
        assertThat(json).contains("\"fee_type_description\":\"CLAIM\"");
        assertThat(json).contains("\"fee_amount\":355.00");
        assertThat(json).contains("\"response_id\":\"12\"");
        assertThat(json).contains("\"response_type\":\"Defence\"");
        assertThat(json).contains("\"party_represented\":\"Yes\"");
        assertThat(json).contains("\"claim_fee_paid_date\":\"2026-02-03T10:00:00Z\"");
        assertThat(json).doesNotContain("\"response_id\":\"13\"");
    }

    @Test
    void shouldSkipClaimPartyWhenPartyIsMissing() {
        stubClaimsForExport(
            List.of(header()),
            List.of(new SdpClaimSnapshotParty(CLAIM_ID, null, PartyRole.DEFENDANT.name(), false)),
            List.of(),
            List.of(),
            List.of()
        );

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        assertThat(uploadedJson()).contains("\"parties\":[]");
    }

    @Test
    void shouldLoadClaimsInChunks() {
        int claimCount = EXPORT_CHUNK_SIZE + 1;
        List<UUID> ids = IntStream.range(0, claimCount).mapToObj(i -> UUID.randomUUID()).toList();
        when(sdpClaimSnapshotJdbcRepository.findIdsForExport()).thenReturn(ids);
        when(sdpClaimSnapshotJdbcRepository.findHeadersByClaimId(any())).thenAnswer(invocation -> {
            Collection<UUID> chunkIds = invocation.getArgument(0);
            return chunkIds.stream().map(ExportServiceTest::header).toList();
        });
        when(sdpClaimSnapshotJdbcRepository.findPartiesByClaimId(any())).thenReturn(List.of());
        when(sdpClaimSnapshotJdbcRepository.findPaymentsByClaimId(any())).thenReturn(List.of());
        when(sdpClaimSnapshotJdbcRepository.findGroundsByClaimId(any())).thenReturn(List.of());
        when(sdpClaimSnapshotJdbcRepository.findResponsesByClaimId(any())).thenReturn(List.of());

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(claimCount);
        verify(sdpClaimSnapshotJdbcRepository, times(2)).findHeadersByClaimId(any());
        verify(sdpClaimSnapshotJdbcRepository, times(2)).findPartiesByClaimId(any());
        verify(sdpClaimSnapshotJdbcRepository, times(2)).findPaymentsByClaimId(any());
        verify(sdpClaimSnapshotJdbcRepository, times(2)).findGroundsByClaimId(any());
        verify(sdpClaimSnapshotJdbcRepository, times(2)).findResponsesByClaimId(any());
        verify(sdpClaimSnapshotJdbcRepository).upsertSnapshots(anyList());
    }

    @Test
    void shouldProcessOnlyIdsReturnedByDeltaQuery() {
        stubDefaultClaim();

        ExportResult result = exportService.exportClaims();

        assertThat(result.recordCount()).isEqualTo(1);
        verify(sdpClaimSnapshotJdbcRepository).findIdsForExport();
        verify(sdpClaimSnapshotJdbcRepository).findHeadersByClaimId(List.of(CLAIM_ID));
        verify(sdpClaimSnapshotJdbcRepository).findPartiesByClaimId(List.of(CLAIM_ID));
        verify(sdpClaimSnapshotJdbcRepository).findPaymentsByClaimId(List.of(CLAIM_ID));
        verify(sdpClaimSnapshotJdbcRepository).findGroundsByClaimId(List.of(CLAIM_ID));
        verify(sdpClaimSnapshotJdbcRepository).findResponsesByClaimId(List.of(CLAIM_ID));
    }

    private String uploadedJson() {
        verify(blobUploader).upload(any(), any(Path.class));
        return uploadedJsonContent;
    }

    private void stubDefaultClaim() {
        stubClaimsForExport(
            List.of(header()),
            List.of(new SdpClaimSnapshotParty(CLAIM_ID, PARTY_ID, PartyRole.DEFENDANT.name(), false)),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private void stubClaimsForExport(List<SdpClaimSnapshotHeader> headers,
                                     List<SdpClaimSnapshotParty> parties,
                                     List<SdpClaimSnapshotPayment> payments,
                                     List<SdpClaimSnapshotGround> grounds,
                                     List<SdpClaimSnapshotResponse> responses) {
        List<UUID> ids = headers.stream().map(SdpClaimSnapshotHeader::claimId).toList();
        when(sdpClaimSnapshotJdbcRepository.findIdsForExport()).thenReturn(ids);
        if (!ids.isEmpty()) {
            when(sdpClaimSnapshotJdbcRepository.findHeadersByClaimId(any())).thenReturn(headers);
            when(sdpClaimSnapshotJdbcRepository.findPartiesByClaimId(any())).thenReturn(parties);
            when(sdpClaimSnapshotJdbcRepository.findPaymentsByClaimId(any())).thenReturn(payments);
            when(sdpClaimSnapshotJdbcRepository.findGroundsByClaimId(any())).thenReturn(grounds);
            when(sdpClaimSnapshotJdbcRepository.findResponsesByClaimId(any())).thenReturn(responses);
        }
    }

    private static SdpClaimSnapshotHeader header() {
        return header(CLAIM_ID);
    }

    private static SdpClaimSnapshotHeader header(UUID claimId) {
        return header(claimId, null);
    }

    private static SdpClaimSnapshotHeader header(UUID claimId, Integer snapshotVersion) {
        return new SdpClaimSnapshotHeader(
            claimId,
            ClaimantType.PROVIDER_OF_SOCIAL_HOUSING.name(),
            VerticalYesNo.NO.name(),
            LanguageUsed.ENGLISH.name(),
            LocalDateTime.of(2026, 2, 1, 14, 30),
            LocalDateTime.of(2026, 2, 5, 9, 0),
            1234567890123456L,
            45,
            null,
            CombinedLicenceType.SECURE_TENANCY.name(),
            NoticeServiceMethod.FIRST_CLASS_POST.name(),
            LocalDate.of(2026, 1, 10),
            snapshotVersion,
            null, null, null, null, null, null, null
        );
    }
}
