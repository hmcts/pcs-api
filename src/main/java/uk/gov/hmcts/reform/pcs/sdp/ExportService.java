package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.SdpClaimSnapshotEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotGround;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotRequestedFee;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extracts claim data from the PCS database, maps it to the SDP contract, and uploads JSON.
 */
@Slf4j
@Service
public class ExportService {

    private final SdpClaimSnapshotJdbcRepository sdpClaimSnapshotJdbcRepository;
    private final SnapshotMapper claimSnapshotMapper;
    private final ClaimSnapshotFileWriter claimSnapshotFileWriter;
    private final Optional<BlobUploader> blobUploader;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;
    private final String environment;
    static final int EXPORT_CHUNK_SIZE = 200;
    private static final String SNAPSHOT_FILENAME_PREFIX = "claim_snapshot_";
    private static final String PREVIEW_FILENAME_PREFIX = SNAPSHOT_FILENAME_PREFIX + "preview_";
    private static final String JSON_EXTENSION = ".json";

    public ExportService(SdpClaimSnapshotJdbcRepository sdpClaimSnapshotJdbcRepository,
                         SnapshotMapper claimSnapshotMapper,
                         ClaimSnapshotFileWriter claimSnapshotFileWriter,
                         Optional<BlobUploader> blobUploader,
                         ObjectMapper objectMapper,
                         @Qualifier("utcClock") Clock utcClock,
                         @Value("${sdp.export.environment}") String environment) {
        this.sdpClaimSnapshotJdbcRepository = sdpClaimSnapshotJdbcRepository;
        this.claimSnapshotMapper = claimSnapshotMapper;
        this.claimSnapshotFileWriter = claimSnapshotFileWriter;
        this.blobUploader = blobUploader;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
        this.environment = environment;
    }

    @Transactional
    public ExportResult exportClaims() {
        String extractDate = LocalDate.now(utcClock).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<UUID> ids = sdpClaimSnapshotJdbcRepository.findIdsForExport();
        log.info("SDP claim snapshot export started: {} claim(s)", ids.size());
        final String publishedDate = Instant.now(utcClock).toString();
        LocalDateTime now = LocalDateTime.now(utcClock);
        String filename = snapshotFilename(extractDate);
        Path snapshotFile = createSnapshotFile();
        try {
            int recordCount = writeSnapshotFile(snapshotFile, ids, extractDate, publishedDate, now);
            if (recordCount > 0) {
                blobUploader.ifPresent(uploader -> uploader.upload(filename, snapshotFile));
            }
            log.info("SDP claim snapshot export complete: {} record(s) -> {}", recordCount, filename);
            return new ExportResult(filename, recordCount);
        } finally {
            deleteSnapshotFile(snapshotFile);
        }
    }

    private String snapshotFilename(String extractDate) {
        String prefix = "preview".equals(environment) ? PREVIEW_FILENAME_PREFIX : SNAPSHOT_FILENAME_PREFIX;
        return prefix + extractDate + JSON_EXTENSION;
    }

    private int writeSnapshotFile(Path snapshotFile,
                                  List<UUID> ids,
                                  String extractDate,
                                  String publishedDate,
                                  LocalDateTime now) {
        try (OutputStream out = Files.newOutputStream(snapshotFile);
             JsonGenerator generator = objectMapper.getFactory().createGenerator(out)) {
            claimSnapshotFileWriter.writeStartArray(generator);
            int recordCount = 0;
            List<SdpClaimSnapshotEntity> changed = new ArrayList<>();
            for (int i = 0; i < ids.size(); i += EXPORT_CHUNK_SIZE) {
                List<UUID> chunk = ids.subList(i, Math.min(i + EXPORT_CHUNK_SIZE, ids.size()));
                List<SdpClaimSnapshotHeader> headers = sdpClaimSnapshotJdbcRepository.findHeadersByClaimId(chunk);
                Map<UUID, List<SdpClaimSnapshotParty>> parties = groupByClaimId(
                    sdpClaimSnapshotJdbcRepository.findPartiesByClaimId(chunk), SdpClaimSnapshotParty::claimId);
                Map<UUID, List<SdpClaimSnapshotPayment>> payments = groupByClaimId(
                    sdpClaimSnapshotJdbcRepository.findPaymentsByClaimId(chunk), SdpClaimSnapshotPayment::claimId);
                Map<UUID, List<SdpClaimSnapshotGround>> grounds = groupByClaimId(
                    sdpClaimSnapshotJdbcRepository.findGroundsByClaimId(chunk), SdpClaimSnapshotGround::claimId);
                Map<UUID, List<SdpClaimSnapshotResponse>> responses = groupByClaimId(
                    sdpClaimSnapshotJdbcRepository.findResponsesByClaimId(chunk), SdpClaimSnapshotResponse::claimId);
                for (SdpClaimSnapshotHeader header : headers) {
                    writeClaim(
                        header,
                        parties.getOrDefault(header.claimId(), List.of()),
                        payments.getOrDefault(header.claimId(), List.of()),
                        grounds.getOrDefault(header.claimId(), List.of()),
                        responses.getOrDefault(header.claimId(), List.of()),
                        extractDate,
                        publishedDate,
                        now,
                        generator,
                        changed
                    );
                    recordCount++;
                }
                log.info(
                    "SDP claim snapshot export progress: {}/{} claim(s)",
                    Math.min(i + EXPORT_CHUNK_SIZE, ids.size()),
                    ids.size()
                );
            }
            sdpClaimSnapshotJdbcRepository.upsertSnapshots(changed);
            claimSnapshotFileWriter.finishArray(generator);
            return recordCount;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write SDP claim snapshot", e);
        }
    }

    private void writeClaim(SdpClaimSnapshotHeader header,
                                List<SdpClaimSnapshotParty> parties,
                                List<SdpClaimSnapshotPayment> payments,
                                List<SdpClaimSnapshotGround> grounds,
                                List<SdpClaimSnapshotResponse> responses,
                                String extractDate,
                                String publishedDate,
                                LocalDateTime now,
                                JsonGenerator generator,
                                List<SdpClaimSnapshotEntity> changed) throws IOException {
        ClaimSnapshotRecord snapshotRecord = mapClaim(header, parties, payments, grounds, responses,
            extractDate, publishedDate);
        int version = header.snapshotVersion() == null ? 1 : header.snapshotVersion() + 1;
        changed.add(SdpClaimSnapshotEntity.builder()
            .claimId(header.claimId())
            .snapshotVersion(version)
            .exportedAt(now)
            .build());
        snapshotRecord.getClaim().setSnapshotVersion(version);
        claimSnapshotFileWriter.writeRecord(generator, snapshotRecord);
    }

    private static Path createSnapshotFile() {
        try {
            return Files.createTempFile(
                "sdp-claim-snapshot-",
                JSON_EXTENSION,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create SDP claim snapshot file", e);
        }
    }

    private void deleteSnapshotFile(Path snapshotFile) {
        try {
            Files.deleteIfExists(snapshotFile);
        } catch (IOException e) {
            log.warn("Failed to delete SDP claim snapshot temp file {}", snapshotFile, e);
        }
    }

    private ClaimSnapshotRecord mapClaim(SdpClaimSnapshotHeader header,
                                         List<SdpClaimSnapshotParty> parties,
                                         List<SdpClaimSnapshotPayment> payments,
                                         List<SdpClaimSnapshotGround> grounds,
                                         List<SdpClaimSnapshotResponse> responses,
                                         String extractDate,
                                         String publishedDate) {
        SnapshotDimensions dimensions = claimSnapshotMapper.mapDimensions(
            header.claimantType(),
            header.againstTrespassers(),
            header.languageUsed(),
            header.tenancyType(),
            header.caseManagementLocation() != null ? String.valueOf(header.caseManagementLocation()) : null,
            header.legislativeCountry()
        );
        dimensions.setPropertyAddress(claimSnapshotMapper.mapPropertyAddress(
            header.addressLine1(),
            header.addressLine2(),
            header.addressLine3(),
            header.postTown(),
            header.county(),
            header.country(),
            header.postcode()
        ));

        SnapshotMilestones milestones = claimSnapshotMapper.mapMilestones(
            header.servingMethod(),
            header.noticeDate() != null ? header.noticeDate().toString() : null,
            formatDateTime(header.claimSubmittedDate()),
            formatDateTime(header.claimIssuedDate()),
            formatDateTime(firstPaidFeeDate(payments))
        );

        return claimSnapshotMapper.mapRecord(
            environment,
            extractDate,
            UUID.randomUUID().toString(),
            publishedDate,
            ClaimSnapshot.builder()
                .claimReference(formatClaimReference(header.caseReference()))
                .snapshotVersion(0)
                .recordedDate(formatDateTime(header.claimSubmittedDate()))
                .dimensions(dimensions)
                .milestones(milestones)
                .parties(mapParties(parties, header.claimantType(), header.languageUsed()))
                .requestedFees(mapRequestedFees(payments))
                .grounds(mapGrounds(grounds))
                .responses(mapResponses(responses))
                .build()
        );
    }

    private List<SnapshotParty> mapParties(List<SdpClaimSnapshotParty> parties, String claimantType,
                                          String languageUsed) {
        if (parties == null || parties.isEmpty()) {
            return List.of();
        }
        List<SnapshotParty> mapped = new ArrayList<>();
        for (SdpClaimSnapshotParty party : parties) {
            if (party.partyId() == null) {
                continue;
            }
            mapped.add(claimSnapshotMapper.mapParty(
                party.partyId().toString(),
                party.role(),
                claimantType,
                party.represented(),
                languageUsed
            ));
        }
        return mapped;
    }

    private List<SnapshotRequestedFee> mapRequestedFees(List<SdpClaimSnapshotPayment> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        List<SnapshotRequestedFee> mapped = new ArrayList<>();
        for (SdpClaimSnapshotPayment payment : payments) {
            mapped.add(claimSnapshotMapper.mapRequestedFee(payment, formatDateTime(payment.requestDate())));
        }
        return mapped;
    }

    private List<SnapshotGround> mapGrounds(List<SdpClaimSnapshotGround> grounds) {
        if (grounds == null || grounds.isEmpty()) {
            return List.of();
        }
        List<SnapshotGround> mapped = new ArrayList<>();
        for (SdpClaimSnapshotGround ground : grounds) {
            mapped.add(claimSnapshotMapper.mapGround(
                ground.code(),
                ground.description(),
                ground.category()
            ));
        }
        return mapped;
    }

    private List<SnapshotResponse> mapResponses(List<SdpClaimSnapshotResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return List.of();
        }
        List<SnapshotResponse> mapped = new ArrayList<>();
        for (SdpClaimSnapshotResponse response : responses) {
            mapped.add(claimSnapshotMapper.mapResponse(
                response.responseId() != null ? response.responseId().toString() : null,
                response.partyId() != null ? response.partyId().toString() : null,
                response.makeCounterClaim(),
                formatDateTime(response.responseSubmittedDate())
            ));
        }
        return mapped;
    }

    private static LocalDateTime firstPaidFeeDate(List<SdpClaimSnapshotPayment> payments) {
        if (payments == null) {
            return null;
        }
        return payments.stream()
            .filter(payment -> PaymentStatus.PAID.name().equals(payment.paymentStatus()))
            .map(SdpClaimSnapshotPayment::requestDate)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);
    }

    private static <T> Map<UUID, List<T>> groupByClaimId(List<T> values, Function<T, UUID> claimId) {
        return values.stream().collect(Collectors.groupingBy(claimId));
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC).toString();
    }

    private static String formatClaimReference(Long caseReference) {
        return caseReference == null ? null : String.format("%016d", caseReference);
    }
}
