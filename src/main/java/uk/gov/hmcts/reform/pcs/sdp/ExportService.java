package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.SdpClaimSnapshotEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.SdpClaimSnapshotRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotGround;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotPaymentReference;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
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

    private final ClaimRepository claimRepository;
    private final SdpClaimSnapshotReadRepository sdpClaimSnapshotReadRepository;
    private final SnapshotMapper claimSnapshotMapper;
    private final ClaimSnapshotFileWriter claimSnapshotFileWriter;
    private final SdpClaimSnapshotRepository sdpClaimSnapshotRepository;
    private final Optional<BlobUploader> blobUploader;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;
    private final String environment;
    static final int EXPORT_CHUNK_SIZE = 200;

    private final CaseReferenceFormatter caseReferenceFormatter = new CaseReferenceFormatter();

    public ExportService(ClaimRepository claimRepository,
                         SdpClaimSnapshotReadRepository sdpClaimSnapshotReadRepository,
                         SnapshotMapper claimSnapshotMapper,
                         ClaimSnapshotFileWriter claimSnapshotFileWriter,
                         SdpClaimSnapshotRepository sdpClaimSnapshotRepository,
                         Optional<BlobUploader> blobUploader,
                         EntityManager entityManager,
                         ObjectMapper objectMapper,
                         @Qualifier("utcClock") Clock utcClock,
                         @Value("${sdp.export.environment}") String environment) {
        this.claimRepository = claimRepository;
        this.sdpClaimSnapshotReadRepository = sdpClaimSnapshotReadRepository;
        this.claimSnapshotMapper = claimSnapshotMapper;
        this.claimSnapshotFileWriter = claimSnapshotFileWriter;
        this.sdpClaimSnapshotRepository = sdpClaimSnapshotRepository;
        this.blobUploader = blobUploader;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
        this.environment = environment;
    }

    @Transactional
    public ExportResult exportClaims() {
        String extractDate = LocalDate.now(utcClock).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<UUID> ids = claimRepository.findAllIdsForSdpSnapshotExport();
        log.info("SDP claim snapshot export started: {} claim(s)", ids.size());
        final String publishedDate = Instant.now(utcClock).toString();
        LocalDateTime now = LocalDateTime.now(utcClock);
        String filename = "claim_snapshot_" + extractDate + ".json";
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

    private int writeSnapshotFile(Path snapshotFile,
                                  List<UUID> ids,
                                  String extractDate,
                                  String publishedDate,
                                  LocalDateTime now) {
        try (OutputStream out = Files.newOutputStream(snapshotFile);
             JsonGenerator generator = objectMapper.getFactory().createGenerator(out)) {
            claimSnapshotFileWriter.writeStartArray(generator);
            int recordCount = 0;
            for (int i = 0; i < ids.size(); i += EXPORT_CHUNK_SIZE) {
                List<UUID> chunk = ids.subList(i, Math.min(i + EXPORT_CHUNK_SIZE, ids.size()));
                List<SdpClaimSnapshotHeader> headers = sdpClaimSnapshotReadRepository.findHeadersByClaimId(chunk);
                Map<UUID, List<SdpClaimSnapshotParty>> parties = groupByClaimId(
                    sdpClaimSnapshotReadRepository.findPartiesByClaimId(chunk), SdpClaimSnapshotParty::claimId);
                Map<UUID, List<SdpClaimSnapshotPayment>> payments = groupByClaimId(
                    sdpClaimSnapshotReadRepository.findPaymentsByClaimId(chunk), SdpClaimSnapshotPayment::claimId);
                Map<UUID, List<SdpClaimSnapshotGround>> grounds = groupByClaimId(
                    sdpClaimSnapshotReadRepository.findGroundsByClaimId(chunk), SdpClaimSnapshotGround::claimId);
                Map<UUID, List<SdpClaimSnapshotResponse>> responses = groupByClaimId(
                    sdpClaimSnapshotReadRepository.findResponsesByClaimId(chunk), SdpClaimSnapshotResponse::claimId);
                Map<UUID, SdpClaimSnapshotEntity> stored = sdpClaimSnapshotRepository.findAllById(chunk).stream()
                    .collect(Collectors.toMap(SdpClaimSnapshotEntity::getClaimId, Function.identity()));
                List<SdpClaimSnapshotEntity> changed = new ArrayList<>();
                for (SdpClaimSnapshotHeader header : headers) {
                    writeIfChanged(
                        header,
                        parties.getOrDefault(header.claimId(), List.of()),
                        payments.getOrDefault(header.claimId(), List.of()),
                        grounds.getOrDefault(header.claimId(), List.of()),
                        responses.getOrDefault(header.claimId(), List.of()),
                        extractDate,
                        publishedDate,
                        stored,
                        now,
                        generator,
                        changed
                    );
                    recordCount++;
                }
                sdpClaimSnapshotRepository.saveAll(changed);
                entityManager.flush();
                entityManager.clear();
                log.info(
                    "SDP claim snapshot export progress: {}/{} claim(s)",
                    Math.min(i + EXPORT_CHUNK_SIZE, ids.size()),
                    ids.size()
                );
            }
            claimSnapshotFileWriter.finishArray(generator);
            return recordCount;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write SDP claim snapshot", e);
        }
    }

    private void writeIfChanged(SdpClaimSnapshotHeader header,
                                List<SdpClaimSnapshotParty> parties,
                                List<SdpClaimSnapshotPayment> payments,
                                List<SdpClaimSnapshotGround> grounds,
                                List<SdpClaimSnapshotResponse> responses,
                                String extractDate,
                                String publishedDate,
                                Map<UUID, SdpClaimSnapshotEntity> stored,
                                LocalDateTime now,
                                JsonGenerator generator,
                                List<SdpClaimSnapshotEntity> changed) throws IOException {
        ClaimSnapshotRecord snapshotRecord = mapClaim(header, parties, payments, grounds, responses,
            extractDate, publishedDate);
        SdpClaimSnapshotEntity snapshot = stored.get(header.claimId());
        if (snapshot == null) {
            snapshot = SdpClaimSnapshotEntity.builder()
                .claimId(header.claimId())
                .snapshotVersion(1)
                .exportedAt(now)
                .build();
        } else {
            snapshot.setSnapshotVersion(snapshot.getSnapshotVersion() + 1);
            snapshot.setExportedAt(now);
        }
        changed.add(snapshot);
        snapshotRecord.getClaim().setSnapshotVersion(snapshot.getSnapshotVersion());
        claimSnapshotFileWriter.writeRecord(generator, snapshotRecord);
    }

    private static Path createSnapshotFile() {
        try {
            Path snapshotFile = Files.createTempFile("sdp-claim-snapshot-", ".json");
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(snapshotFile, PosixFilePermissions.fromString("rw-------"));
            }
            return snapshotFile;
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
                .claimReference(caseReferenceFormatter.formatCaseReferenceWithDashes(header.caseReference()))
                .snapshotVersion(0)
                .recordedDate(formatDateTime(header.claimSubmittedDate()))
                .dimensions(dimensions)
                .milestones(milestones)
                .parties(mapParties(parties, header.claimantType()))
                .paymentReferences(mapPayments(payments))
                .grounds(mapGrounds(grounds))
                .responses(mapResponses(responses))
                .build()
        );
    }

    private List<SnapshotParty> mapParties(List<SdpClaimSnapshotParty> parties, String claimantType) {
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
                party.represented()
            ));
        }
        return mapped;
    }

    private List<SnapshotPaymentReference> mapPayments(List<SdpClaimSnapshotPayment> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        List<SnapshotPaymentReference> mapped = new ArrayList<>();
        for (SdpClaimSnapshotPayment payment : payments) {
            if (payment.externalReference() == null || payment.externalReference().isBlank()) {
                continue;
            }
            mapped.add(claimSnapshotMapper.mapPayment(payment.externalReference()));
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
}
