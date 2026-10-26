package uk.gov.hmcts.reform.pcs.sdp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.SdpClaimSnapshotEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.SdpClaimSnapshotRepository;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshotRecord;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extracts claim data from the PCS database, maps it to the SDP contract, and writes a JSON file.
 */
@Slf4j
@Service
public class ExportService {

    private final ClaimRepository claimRepository;
    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final SnapshotMapper claimSnapshotMapper;
    private final ClaimSnapshotFileWriter claimSnapshotFileWriter;
    private final SnapshotHasher snapshotHasher;
    private final SdpClaimSnapshotRepository sdpClaimSnapshotRepository;
    private final Optional<BlobUploader> blobUploader;
    private final Clock utcClock;
    private final String environment;
    private final Path outputDirectory;

    public ExportService(ClaimRepository claimRepository,
                         ClaimActivityLogRepository claimActivityLogRepository,
                         SnapshotMapper claimSnapshotMapper,
                         ClaimSnapshotFileWriter claimSnapshotFileWriter,
                         SnapshotHasher snapshotHasher,
                         SdpClaimSnapshotRepository sdpClaimSnapshotRepository,
                         Optional<BlobUploader> blobUploader,
                         @Qualifier("utcClock") Clock utcClock,
                         @Value("${sdp.export.environment}") String environment,
                         @Value("${sdp.export.output-directory}") String outputDirectory) {
        this.claimRepository = claimRepository;
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.claimSnapshotMapper = claimSnapshotMapper;
        this.claimSnapshotFileWriter = claimSnapshotFileWriter;
        this.snapshotHasher = snapshotHasher;
        this.sdpClaimSnapshotRepository = sdpClaimSnapshotRepository;
        this.blobUploader = blobUploader;
        this.utcClock = utcClock;
        this.environment = environment;
        this.outputDirectory = Path.of(outputDirectory);
    }

    @Transactional
    public ExportResult exportClaims() {
        String extractDate = LocalDate.now(utcClock).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<ClaimEntity> claims = claimRepository.findAllForSdpSnapshotExport();
        Map<UUID, SdpClaimSnapshotEntity> stored = sdpClaimSnapshotRepository
            .findAllById(claims.stream().map(ClaimEntity::getId).toList()).stream()
            .collect(Collectors.toMap(SdpClaimSnapshotEntity::getClaimId, Function.identity()));
        Map<UUID, LocalDateTime> publishedDates = findPublishedDates(claims);
        LocalDateTime now = LocalDateTime.now(utcClock);

        List<ClaimSnapshotRecord> records = new ArrayList<>();
        List<SdpClaimSnapshotEntity> changed = new ArrayList<>();
        for (ClaimEntity claim : claims) {
            addIfChanged(claim, extractDate, publishedDates, stored, now, records, changed);
        }
        return writeAndUpload(extractDate, records, changed);
    }

    private void addIfChanged(ClaimEntity claim,
                              String extractDate,
                              Map<UUID, LocalDateTime> publishedDates,
                              Map<UUID, SdpClaimSnapshotEntity> stored,
                              LocalDateTime now,
                              List<ClaimSnapshotRecord> records,
                              List<SdpClaimSnapshotEntity> changed) {
        ClaimSnapshotRecord snapshotRecord = mapClaim(claim, extractDate, publishedDates);
        String hash = snapshotHasher.hash(snapshotRecord.getClaim());
        SdpClaimSnapshotEntity snapshot = stored.get(claim.getId());
        if (snapshot == null) {
            snapshot = SdpClaimSnapshotEntity.builder()
                .claimId(claim.getId())
                .snapshotVersion(1)
                .snapshotHash(hash)
                .exportedAt(now)
                .build();
            changed.add(snapshot);
        } else if (!snapshot.getSnapshotHash().equals(hash)) {
            snapshot.setSnapshotVersion(snapshot.getSnapshotVersion() + 1);
            snapshot.setSnapshotHash(hash);
            snapshot.setExportedAt(now);
            changed.add(snapshot);
        } else if (snapshot.getExportedAt() != null
            && snapshot.getExportedAt().toLocalDate().equals(now.toLocalDate())) {
            snapshotRecord.getClaim().setSnapshotVersion(snapshot.getSnapshotVersion());
            records.add(snapshotRecord);
            return;
        } else {
            return;
        }
        snapshotRecord.getClaim().setSnapshotVersion(snapshot.getSnapshotVersion());
        records.add(snapshotRecord);
    }

    private ExportResult writeAndUpload(String extractDate,
                                        List<ClaimSnapshotRecord> records,
                                        List<SdpClaimSnapshotEntity> changed) {
        String filename = "claim_snapshot_" + extractDate + ".json";
        Path outputPath = outputDirectory.resolve(filename);
        byte[] json = claimSnapshotFileWriter.write(records, outputPath);
        if (!records.isEmpty()) {
            blobUploader.ifPresent(uploader -> uploader.upload(filename, json));
        }
        sdpClaimSnapshotRepository.saveAll(changed);

        log.info("SDP claim snapshot export complete: {} record(s) -> {}", records.size(), outputPath);
        return new ExportResult(outputPath, records.size());
    }

    private ClaimSnapshotRecord mapClaim(ClaimEntity claim, String extractDate,
                                         Map<UUID, LocalDateTime> publishedDates) {
        PcsCaseEntity pcsCase = claim.getPcsCase();
        NoticeOfPossessionEntity notice = claim.getNoticeOfPossession();
        TenancyLicenceEntity tenancy = pcsCase != null ? pcsCase.getTenancyLicence() : null;

        SnapshotDimensions dimensions = claimSnapshotMapper.mapDimensions(
            enumName(claim.getClaimantType()),
            enumName(claim.getAgainstTrespassers()),
            enumName(claim.getLanguageUsed()),
            tenancy != null ? enumName(tenancy.getType()) : null,
            pcsCase != null && pcsCase.getCaseManagementLocation() != null
                ? String.valueOf(pcsCase.getCaseManagementLocation())
                : null
        );

        SnapshotMilestones milestones = claimSnapshotMapper.mapMilestones(
            notice != null ? enumName(notice.getServingMethod()) : null,
            notice != null && notice.getNoticeDate() != null ? notice.getNoticeDate().toString() : null,
            formatDateTime(claim.getClaimSubmittedDate()),
            formatDateTime(claim.getClaimIssuedDate())
        );

        List<SnapshotParty> parties = mapParties(claim.getClaimParties());

        String claimReference = pcsCase != null && pcsCase.getCaseReference() != null
            ? String.valueOf(pcsCase.getCaseReference())
            : null;

        UUID caseId = pcsCase != null ? pcsCase.getId() : null;

        return claimSnapshotMapper.mapRecord(
            environment,
            extractDate,
            UUID.randomUUID().toString(),
            resolvePublishedDate(publishedDates.get(caseId)),
            ClaimSnapshot.builder()
                .claimReference(claimReference)
                .snapshotVersion(0)
                .recordedDate(formatDateTime(claim.getClaimSubmittedDate()))
                .dimensions(dimensions)
                .milestones(milestones)
                .parties(parties)
                .build()
        );
    }

    private List<SnapshotParty> mapParties(List<ClaimPartyEntity> claimParties) {
        if (claimParties == null || claimParties.isEmpty()) {
            return List.of();
        }
        List<SnapshotParty> parties = new ArrayList<>();
        for (ClaimPartyEntity claimParty : claimParties) {
            PartyEntity party = claimParty.getParty();
            if (party == null) {
                continue;
            }
            parties.add(claimSnapshotMapper.mapParty(
                party.getId() != null ? party.getId().toString() : null,
                enumName(claimParty.getRole()),
                party.getOrgName(),
                party.getFirstName(),
                party.getLastName(),
                party.getEmailAddress()
            ));
        }
        return parties;
    }

    private Map<UUID, LocalDateTime> findPublishedDates(List<ClaimEntity> claims) {
        List<UUID> caseIds = claims.stream()
            .map(ClaimEntity::getPcsCase)
            .filter(Objects::nonNull)
            .map(PcsCaseEntity::getId)
            .toList();
        return claimActivityLogRepository
            .findAllByPcsCase_IdInAndActivityType(caseIds, ClaimActivityType.DOCUMENTS_CREATED).stream()
            .collect(Collectors.toMap(
                logEntry -> logEntry.getPcsCase().getId(),
                ClaimActivityLogEntity::getCreatedAt,
                BinaryOperator.maxBy(Comparator.naturalOrder())
            ));
    }

    private String resolvePublishedDate(LocalDateTime createdAt) {
        return createdAt != null
            ? createdAt.toInstant(ZoneOffset.UTC).toString()
            : Instant.now(utcClock).toString();
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString().replace('T', ' ');
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
