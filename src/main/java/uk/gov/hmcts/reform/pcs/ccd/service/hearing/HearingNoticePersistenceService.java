package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingNoticePartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.hearingnotice.HearingNoticePayload;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatOrdinalDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatShortTime;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

/**
 * Database side of the notice of hearing, kept to two short transactions so no connection is held while the
 * notices are generated. Recipients are every claimant and defendant, or only the selected parties for a
 * without-notice hearing.
 */
@Service
public class HearingNoticePersistenceService {

    private static final String OUTPUT_FILENAME_FORMAT = "Notice of hearing %d";

    private final HearingRepository hearingRepository;
    private final LocationReferenceService locationReferenceService;
    private final DocumentImportService documentImportService;
    private final DocumentNameService documentNameService;
    private final ClaimActivityLogService claimActivityLogService;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final RecipientAddressResolver recipientAddressResolver;
    private final DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    private final AddressMapper addressMapper;
    private final NotificationService notificationService;
    private final String portalUrl;

    public HearingNoticePersistenceService(HearingRepository hearingRepository,
                                           LocationReferenceService locationReferenceService,
                                           DocumentImportService documentImportService,
                                           DocumentNameService documentNameService,
                                           ClaimActivityLogService claimActivityLogService,
                                           CaseReferenceFormatter caseReferenceFormatter,
                                           RecipientAddressResolver recipientAddressResolver,
                                           DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver,
                                           AddressMapper addressMapper,
                                           NotificationService notificationService,
                                           @Value("${frontend.url}") String portalUrl) {
        this.hearingRepository = hearingRepository;
        this.locationReferenceService = locationReferenceService;
        this.documentImportService = documentImportService;
        this.documentNameService = documentNameService;
        this.claimActivityLogService = claimActivityLogService;
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.recipientAddressResolver = recipientAddressResolver;
        this.defenceCorrespondenceAddressResolver = defenceCorrespondenceAddressResolver;
        this.addressMapper = addressMapper;
        this.notificationService = notificationService;
        this.portalUrl = portalUrl;
    }

    /**
     * Builds the data needed to generate one notice per recipient, or none when the hearing doesn't issue a notice
     * or has been cancelled.
     */
    @Transactional(readOnly = true)
    public List<HearingNoticeRenderContext> buildNoticeContexts(Integer hearingId) {
        HearingEntity hearing = loadHearing(hearingId);
        if (hearing.getIssueNotice() != VerticalYesNo.YES || Boolean.TRUE.equals(hearing.getCancelled())) {
            return List.of();
        }
        PcsCaseEntity pcsCase = hearing.getPcsCase();
        String courtName = resolveCourtName(pcsCase);
        ClaimEntity claim = pcsCase.getMainClaim();
        List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        List<PartyEntity> defendants = partiesByRole(claim, PartyRole.DEFENDANT);
        String allClaimantNames = joinNames(claimants);
        String allDefendantNames = joinNames(defendants);
        String filenamePrefix = OUTPUT_FILENAME_FORMAT.formatted(hearing.getNoticeNumber());

        List<HearingNoticeRenderContext> contexts = new ArrayList<>();
        for (PartyEntity recipient : resolveRecipients(hearing, claimants, defendants)) {
            boolean isClaimant = claimants.contains(recipient);
            String recipientName = recipientAddressResolver.resolveDisplayName(recipient);
            // The recipient alone on their own side of the heading, every party on the other side.
            String claimantName = isClaimant ? recipientName : allClaimantNames;
            String defendantName = isClaimant ? allDefendantNames : recipientName;
            HearingNoticePayload payload = buildPayload(pcsCase, hearing, resolveRecipientAddress(pcsCase, recipient,
                isClaimant), recipientName, claimantName, defendantName, courtName);
            String filename = documentNameService.appendPartyPostfix(filenamePrefix, claim, recipient.getId());
            contexts.add(new HearingNoticeRenderContext(recipient.getId(), payload, filename));
        }
        return contexts;
    }

    /**
     * Adds each generated notice to the case file, linked to the hearing and its recipient, then queues an email
     * to each recipient. Emails are only queued here, once every notice has been generated, so a failed attempt
     * emails nobody.
     */
    @Transactional
    public void attachNoticesAndQueueEmails(Integer hearingId, Map<UUID, String> documentUrlsByPartyId) {
        HearingEntity hearing = loadHearing(hearingId);
        PcsCaseEntity pcsCase = hearing.getPcsCase();
        ClaimEntity claim = pcsCase.getMainClaim();
        List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        List<PartyEntity> recipients = resolveRecipients(hearing, claimants, partiesByRole(claim, PartyRole.DEFENDANT))
            .stream()
            .filter(recipient -> documentUrlsByPartyId.containsKey(recipient.getId()))
            .toList();

        for (PartyEntity recipient : recipients) {
            DocumentEntity notice = documentImportService.addDocumentToCase(
                pcsCase, documentUrlsByPartyId.get(recipient.getId()), CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS);
            notice.setType(DocumentType.NOTICE_OF_HEARING);
            notice.setParty(recipient);
            hearing.addNoticeDocument(notice);
            claimActivityLogService.logGenerationSuccess(pcsCase, recipient);
            PartyRole role = claimants.contains(recipient) ? PartyRole.CLAIMANT : PartyRole.DEFENDANT;
            notificationService.sendHearingNoticeEmail(pcsCase, recipient, role);
        }
    }

    private HearingEntity loadHearing(Integer hearingId) {
        return hearingRepository.findById(hearingId)
            .orElseThrow(() -> new IllegalStateException("No hearing found for id: " + hearingId));
    }

    private String resolveCourtName(PcsCaseEntity pcsCase) {
        Integer baseLocation = pcsCase.getBaseLocation();
        if (baseLocation == null) {
            throw new IllegalStateException(
                "No base location for case " + pcsCase.getCaseReference() + "; cannot generate notice of hearing");
        }
        List<CourtVenue> courtVenues = locationReferenceService.getCourtVenues(List.of(baseLocation));
        if (courtVenues == null || courtVenues.isEmpty()) {
            throw new IllegalStateException(
                "No court venue found for base location " + baseLocation + " on case "
                    + pcsCase.getCaseReference() + "; cannot generate notice of hearing");
        }
        return courtVenues.getFirst().courtName();
    }

    // A without-notice hearing goes only to the parties selected on it, otherwise to every claimant and defendant.
    private List<PartyEntity> resolveRecipients(HearingEntity hearing, List<PartyEntity> claimants,
                                                List<PartyEntity> defendants) {
        if (hearing.getIsWithoutNotice() == VerticalYesNo.YES) {
            return hearing.getHearingNoticeParties().stream()
                .map(HearingNoticePartyEntity::getParty)
                .filter(party -> !party.isRemoved())
                .toList();
        }
        List<PartyEntity> allParties = new ArrayList<>(claimants);
        allParties.addAll(defendants);
        return allParties;
    }

    private HearingNoticePayload buildPayload(PcsCaseEntity pcsCase, HearingEntity hearing, AddressUK address,
                                              String recipientName, String claimantName, String defendantName,
                                              String courtName) {
        return HearingNoticePayload.builder()
            .recipientName(recipientName)
            .recipientAddressLine1(address == null ? null : address.getAddressLine1())
            .recipientAddressLine2(address == null ? null : address.getAddressLine2())
            .recipientAddressLine3(address == null ? null : address.getAddressLine3())
            .recipientPostTown(address == null ? null : address.getPostTown())
            .recipientCounty(address == null ? null : address.getCounty())
            .recipientPostcode(address == null ? null : address.getPostCode())
            .hasAddressLine2(address != null && StringUtils.isNotBlank(address.getAddressLine2()))
            .hasAddressLine3(address != null && StringUtils.isNotBlank(address.getAddressLine3()))
            .hasCounty(address != null && StringUtils.isNotBlank(address.getCounty()))
            .courtName(courtName)
            .caseReference(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()))
            .claimantName(claimantName)
            .defendantName(defendantName)
            .hearingTime(formatShortTime(hearing.getHearingDate()))
            .hearingDate(formatOrdinalDate(hearing.getHearingDate().toLocalDate()))
            .url(portalUrl)
            .build();
    }

    private AddressUK resolveRecipientAddress(PcsCaseEntity pcsCase, PartyEntity recipient, boolean isClaimant) {
        if (!isClaimant) {
            return defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(
                recipient, pcsCase.getPropertyAddress());
        }
        AddressEntity address = recipientAddressResolver.resolvePostalAddress(
            recipient, PartyRole.CLAIMANT, pcsCase.getPropertyAddress());
        return address == null ? null : addressMapper.toAddressUK(address);
    }

    private String joinNames(List<PartyEntity> parties) {
        return String.join(", ", parties.stream().map(recipientAddressResolver::resolveDisplayName).toList());
    }
}
