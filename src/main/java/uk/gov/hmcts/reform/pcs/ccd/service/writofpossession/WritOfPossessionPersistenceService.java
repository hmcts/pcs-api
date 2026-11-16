package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.enforcement.EnforcementDefendant;
import uk.gov.hmcts.reform.pcs.document.model.writofpossession.WritOfPossessionPayload;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WritOfPossessionPersistenceService {

    static final String COMPLETED_BY_CLAIMANT = "Claimant";
    static final String COMPLETED_BY_LEGAL_REPRESENTATIVE = "Claimant’s legal representative";
    static final String HCEO_SELECTED_BY_NICE = "The National Information Centre for Enforcement will choose a "
        + "High Court Enforcement Officer for you";

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final DocumentImportService documentImportService;
    private final ClaimActivityLogService claimActivityLogService;
    private final PartyService partyService;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    private final RecipientAddressResolver recipientAddressResolver;
    private final NotificationService notificationService;

    /**
     * Builds the data needed to render the writ, or nothing when the order isn't a writ or its writ has already been
     * generated (so a retried or re-scheduled task doesn't attach a second copy).
     */
    @Transactional(readOnly = true)
    public Optional<WritOfPossessionPayload> buildPayload(UUID enforcementOrderId) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        if (enforcementOrder.getWritDetails() == null || hasWritDocument(enforcementOrder)) {
            return Optional.empty();
        }
        return Optional.of(toPayload(enforcementOrder));
    }

    @Transactional
    public void attachDocumentAndQueueEmail(UUID enforcementOrderId, String documentUrl) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);

        DocumentEntity document = documentImportService.addDocumentToCase(
            pcsCase, documentUrl, CaseFileCategory.ENFORCEMENT);
        document.setType(DocumentType.WRIT_OF_POSSESSION);
        enforcementOrder.addDocuments(List.of(document));

        claimActivityLogService.logGenerationSuccess(pcsCase, claimant);
        notificationService.sendWritOfPossessionEmail(claimant);
    }

    private WritOfPossessionPayload toPayload(EnforcementOrderEntity enforcementOrder) {
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();
        WritEntity writ = enforcementOrder.getWritDetails();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);
        String propertyAddress = formatAddress(pcsCase.getPropertyAddress());
        StatementOfTruthEntity statementOfTruth = enforcementOrder.getStatementOfTruth();

        return WritOfPossessionPayload.builder()
            // Title block
            .referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()))
            .caseName(caseNameFormatter.formatCaseName(pcsCase))
            // Claimant details
            .claimantName(partyService.getPartyName(claimant))
            .claimantAddress(formatAddress(recipientAddressResolver.resolvePostalAddress(
                claimant, PartyRole.CLAIMANT, pcsCase.getPropertyAddress())))
            .claimantTelephoneNumber(claimant.getPhoneNumber())
            .claimantEmailAddress(claimant.getEmailAddress())
            // TODO: confirm the source of the claimant / applicant reference
            .claimantReference(null)
            // Defendant details
            .defendants(buildDefendants(enforcementOrder, pcsCase.getPropertyAddress()))
            .propertyAddress(propertyAddress)
            // Application details
            .possessionOrderType(null)
            .possessionOrderDate(null)
            .breathingSpaceAnswer(writ.getDefendantInBreathingSpace() == null
                                      ? null : writ.getDefendantInBreathingSpace().getLabel())
            .highCourtPermissionAnswer(writ.getHasClaimTransferredToHighCourt() == null
                                           ? null : writ.getHasClaimTransferredToHighCourt().getValue())
            // TODO: the date High Court permission was given is not captured yet
            .highCourtPermissionOrderDate(null)
            // High Court Enforcement Officer
            .hceoHiredAnswer(writ.getHasHiredHighCourtEnforcementOfficer() == null
                                 ? null : writ.getHasHiredHighCourtEnforcementOfficer().getLabel())
            .hceoName(writ.getHasHiredHighCourtEnforcementOfficer() == VerticalYesNo.YES
                          ? writ.getHceoDetails() : null)
            .hceoSelectionOutcome(writ.getHasHiredHighCourtEnforcementOfficer() == VerticalYesNo.NO
                                      ? HCEO_SELECTED_BY_NICE : null)
            // Statement of truth
            .statementOfTruthCompletedBy(completedBy(statementOfTruth.getCompletedBy()))
            .signatoryFullName(statementOfTruth.getFullName())
            .signatoryPosition(statementOfTruth.getPositionHeld())
            .signatoryOrganisation(statementOfTruth.getFirmName())
            .submissionDate(statementOfTruth.getCompletedDate().toLocalDate())
            .build();
    }

    private List<EnforcementDefendant> buildDefendants(EnforcementOrderEntity enforcementOrder,
                                                            AddressEntity propertyAddress) {
        return enforcementOrder.getSelectedDefendants().stream()
            .map(SelectedDefendantEntity::getParty)
            .map(defendant -> EnforcementDefendant.builder()
                .name(partyService.getPartyName(defendant))
                .address(formatAddress(defenceCorrespondenceAddressResolver
                                           .resolveCorrespondenceAddress(defendant, propertyAddress)))
                .build())
            .toList();
    }

    private String formatAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        return formatAddress(addressMapper.toAddressUK(address));
    }

    private String formatAddress(AddressUK address) {
        if (address == null) {
            return null;
        }
        return addressFormatter.formatFullAddress(address, AddressFormatter.NEWLINE_DELIMITER);
    }

    private String completedBy(StatementOfTruthCompletedBy completedBy) {
        if (completedBy == null) {
            return null;
        }
        return switch (completedBy) {
            case CLAIMANT -> COMPLETED_BY_CLAIMANT;
            case LEGAL_REPRESENTATIVE -> COMPLETED_BY_LEGAL_REPRESENTATIVE;
        };
    }

    private boolean hasWritDocument(EnforcementOrderEntity enforcementOrder) {
        return enforcementOrder.getDocuments().stream()
            .anyMatch(document -> document.getType() == DocumentType.WRIT_OF_POSSESSION);
    }

    private EnforcementOrderEntity loadEnforcementOrder(UUID enforcementOrderId) {
        return enforcementOrderRepository.findById(enforcementOrderId)
            .orElseThrow(() -> new EnforcementApplicationNotFoundException(
                "Unable to find Enforcement app with ID " + enforcementOrderId));
    }
}
