package uk.gov.hmcts.reform.pcs.sdp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.SdpClaimSnapshotEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
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
import java.util.Set;
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
    private final SnapshotMapper claimSnapshotMapper;
    private final ClaimSnapshotFileWriter claimSnapshotFileWriter;
    private final SnapshotHasher snapshotHasher;
    private final SdpClaimSnapshotRepository sdpClaimSnapshotRepository;
    private final Optional<BlobUploader> blobUploader;
    private final Clock utcClock;
    private final String environment;
    private final CaseReferenceFormatter caseReferenceFormatter = new CaseReferenceFormatter();

    public ExportService(ClaimRepository claimRepository,
                         SnapshotMapper claimSnapshotMapper,
                         ClaimSnapshotFileWriter claimSnapshotFileWriter,
                         SnapshotHasher snapshotHasher,
                         SdpClaimSnapshotRepository sdpClaimSnapshotRepository,
                         Optional<BlobUploader> blobUploader,
                         @Qualifier("utcClock") Clock utcClock,
                         @Value("${sdp.export.environment}") String environment) {
        this.claimRepository = claimRepository;
        this.claimSnapshotMapper = claimSnapshotMapper;
        this.claimSnapshotFileWriter = claimSnapshotFileWriter;
        this.snapshotHasher = snapshotHasher;
        this.sdpClaimSnapshotRepository = sdpClaimSnapshotRepository;
        this.blobUploader = blobUploader;
        this.utcClock = utcClock;
        this.environment = environment;
    }

    @Transactional
    public ExportResult exportClaims() {
        String extractDate = LocalDate.now(utcClock).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<ClaimEntity> claims = claimRepository.findAllForSdpSnapshotExport();
        Map<UUID, SdpClaimSnapshotEntity> stored = sdpClaimSnapshotRepository
            .findAllById(claims.stream().map(ClaimEntity::getId).toList()).stream()
            .collect(Collectors.toMap(SdpClaimSnapshotEntity::getClaimId, Function.identity()));
        final String publishedDate = Instant.now(utcClock).toString();
        LocalDateTime now = LocalDateTime.now(utcClock);

        List<ClaimSnapshotRecord> records = new ArrayList<>();
        List<SdpClaimSnapshotEntity> changed = new ArrayList<>();
        for (ClaimEntity claim : claims) {
            addIfChanged(claim, extractDate, publishedDate, stored, now, records, changed);
        }
        return writeAndUpload(extractDate, records, changed);
    }

    private void addIfChanged(ClaimEntity claim,
                              String extractDate,
                              String publishedDate,
                              Map<UUID, SdpClaimSnapshotEntity> stored,
                              LocalDateTime now,
                              List<ClaimSnapshotRecord> records,
                              List<SdpClaimSnapshotEntity> changed) {
        ClaimSnapshotRecord snapshotRecord = mapClaim(claim, extractDate, publishedDate);
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
        byte[] json = claimSnapshotFileWriter.write(records);
        if (!records.isEmpty()) {
            blobUploader.ifPresent(uploader -> uploader.upload(filename, json));
        }
        sdpClaimSnapshotRepository.saveAll(changed);

        log.info("SDP claim snapshot export complete: {} record(s) -> {}", records.size(), filename);
        return new ExportResult(filename, records.size());
    }

    private ClaimSnapshotRecord mapClaim(ClaimEntity claim, String extractDate, String publishedDate) {
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
                : null,
            pcsCase != null ? enumName(pcsCase.getLegislativeCountry()) : null
        );

        SnapshotMilestones milestones = claimSnapshotMapper.mapMilestones(
            notice != null ? enumName(notice.getServingMethod()) : null,
            notice != null && notice.getNoticeDate() != null ? notice.getNoticeDate().toString() : null,
            formatDateTime(claim.getClaimSubmittedDate()),
            formatDateTime(claim.getClaimIssuedDate()),
            formatDateTime(firstPaidFeeDate(claim.getFeePayments()))
        );

        List<SnapshotParty> parties = mapParties(claim.getClaimParties(), enumName(claim.getClaimantType()));

        String claimReference = caseReferenceFormatter
            .formatCaseReferenceWithDashes(pcsCase != null ? pcsCase.getCaseReference() : null);

        return claimSnapshotMapper.mapRecord(
            environment,
            extractDate,
            UUID.randomUUID().toString(),
            publishedDate,
            ClaimSnapshot.builder()
                .claimReference(claimReference)
                .snapshotVersion(0)
                .recordedDate(formatDateTime(claim.getClaimSubmittedDate()))
                .dimensions(dimensions)
                .milestones(milestones)
                .parties(parties)
                .paymentReferences(mapPayments(claim.getFeePayments()))
                .grounds(mapGrounds(claim.getClaimGrounds()))
                .responses(mapResponses(pcsCase != null ? pcsCase.getDefendantResponses() : null, claim.getId()))
                .build()
        );
    }

    private List<SnapshotParty> mapParties(List<ClaimPartyEntity> claimParties, String claimantType) {
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
                claimantType,
                isRepresented(party)
            ));
        }
        return parties;
    }

    private List<SnapshotPaymentReference> mapPayments(List<FeePaymentEntity> feePayments) {
        if (feePayments == null || feePayments.isEmpty()) {
            return List.of();
        }
        List<SnapshotPaymentReference> payments = new ArrayList<>();
        for (FeePaymentEntity feePayment : feePayments) {
            if (feePayment.getExternalReference() == null || feePayment.getExternalReference().isBlank()) {
                continue;
            }
            payments.add(claimSnapshotMapper.mapPayment(feePayment.getExternalReference()));
        }
        return payments;
    }

    private List<SnapshotGround> mapGrounds(Set<ClaimGroundEntity> claimGrounds) {
        if (claimGrounds == null || claimGrounds.isEmpty()) {
            return List.of();
        }
        List<SnapshotGround> grounds = new ArrayList<>();
        for (ClaimGroundEntity ground : claimGrounds) {
            grounds.add(claimSnapshotMapper.mapGround(
                ground.getCode(),
                ground.getDescription(),
                enumName(ground.getCategory())
            ));
        }
        return grounds;
    }

    private List<SnapshotResponse> mapResponses(List<DefendantResponseEntity> defendantResponses, UUID claimId) {
        if (defendantResponses == null || defendantResponses.isEmpty()) {
            return List.of();
        }
        List<SnapshotResponse> responses = new ArrayList<>();
        for (DefendantResponseEntity response : defendantResponses) {
            if (response.getClaim() != null && claimId != null
                && !claimId.equals(response.getClaim().getId())) {
                continue;
            }
            PartyEntity party = response.getParty();
            responses.add(claimSnapshotMapper.mapResponse(
                response.getId() != null ? response.getId().toString() : null,
                party != null && party.getId() != null ? party.getId().toString() : null,
                enumName(response.getMakeCounterClaim()),
                formatDateTime(response.getResponseSubmittedDate())
            ));
        }
        return responses;
    }

    private static boolean isRepresented(PartyEntity party) {
        if (party.getClaimPartyOrganisationList() == null) {
            return false;
        }
        return party.getClaimPartyOrganisationList().stream()
            .anyMatch(link -> YesOrNo.YES.equals(link.getActive()));
    }

    private static LocalDateTime firstPaidFeeDate(List<FeePaymentEntity> feePayments) {
        if (feePayments == null) {
            return null;
        }
        return feePayments.stream()
            .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PAID)
            .map(FeePaymentEntity::getRequestDate)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC).toString();
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
