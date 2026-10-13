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
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
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
import java.util.UUID;

/**
 * Extracts claim data from the PCS database, maps it to the SDP contract, and writes a JSON file.
 */
@Slf4j
@Service
public class ExportService {

    private static final int DEFAULT_SNAPSHOT_VERSION = 1;

    private final ClaimRepository claimRepository;
    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final SnapshotMapper claimSnapshotMapper;
    private final FileWriter claimSnapshotFileWriter;
    private final BlobUploader blobUploader;
    private final Clock utcClock;
    private final String environment;
    private final Path outputDirectory;

    public ExportService(ClaimRepository claimRepository,
                         ClaimActivityLogRepository claimActivityLogRepository,
                         SnapshotMapper claimSnapshotMapper,
                         FileWriter claimSnapshotFileWriter,
                         BlobUploader blobUploader,
                         @Qualifier("utcClock") Clock utcClock,
                         @Value("${sdp.export.environment}") String environment,
                         @Value("${sdp.export.output-directory}") String outputDirectory) {
        this.claimRepository = claimRepository;
        this.claimActivityLogRepository = claimActivityLogRepository;
        this.claimSnapshotMapper = claimSnapshotMapper;
        this.claimSnapshotFileWriter = claimSnapshotFileWriter;
        this.blobUploader = blobUploader;
        this.utcClock = utcClock;
        this.environment = environment;
        this.outputDirectory = Path.of(outputDirectory);
    }

    /**
     * Runs a full claim snapshot extract and writes JSON under the configured output directory.
     */
    @Transactional(readOnly = true)
    public ExportResult exportAllClaims() {
        List<ClaimEntity> claims = claimRepository.findAllForSdpSnapshotExport();
        String extractDate = LocalDate.now(utcClock).format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<ClaimSnapshotRecord> records = new ArrayList<>();
        for (ClaimEntity claim : claims) {
            records.add(mapClaim(claim, extractDate));
        }

        String filename = "claim_snapshot_export_" + extractDate + ".json";
        Path outputPath = outputDirectory.resolve(filename);
        claimSnapshotFileWriter.write(records, outputPath);
        blobUploader.upload(outputPath);

        log.info("SDP claim snapshot export complete: {} record(s) -> {}", records.size(), outputPath);
        return new ExportResult(outputPath, records.size());
    }

    private ClaimSnapshotRecord mapClaim(ClaimEntity claim, String extractDate) {
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
            resolvePublishedDate(caseId),
            claimReference,
            DEFAULT_SNAPSHOT_VERSION,
            formatDateTime(claim.getClaimSubmittedDate()),
            dimensions,
            milestones,
            parties
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

    private String resolvePublishedDate(UUID caseId) {
        if (caseId == null) {
            return Instant.now(utcClock).toString();
        }
        return claimActivityLogRepository.findAllByPcsCase_Id(caseId).stream()
            .filter(logEntry -> logEntry.getActivityType() == ClaimActivityType.DOCUMENTS_CREATED)
            .map(ClaimActivityLogEntity::getCreatedAt)
            .max(Comparator.naturalOrder())
            .map(createdAt -> createdAt.toInstant(ZoneOffset.UTC).toString())
            .orElseGet(() -> Instant.now(utcClock).toString());
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString().replace('T', ' ');
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
