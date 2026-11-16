package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

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
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
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
import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

@Service
@AllArgsConstructor
public class WarrantOfPossessionPersistenceService {

    static final String APPLICATION_TYPE = "Warrant of Possession";
    static final String SUSPENDED_ORDER = "Suspended order";
    static final String OTHER_ORDER = "Other";
    static final String COMPLETED_BY_CLAIMANT = "Claimant";
    static final String COMPLETED_BY_LEGAL_REPRESENTATIVE = "Claimant’s legal representative";

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

    /**
     * Builds the data needed to render the warrant, or nothing when the order isn't a warrant or its warrant has
     * already been generated (so a retried or re-scheduled task doesn't attach a second copy).
     */
    @Transactional(readOnly = true)
    public Optional<WarrantOfPossessionPayload> buildPayload(UUID enforcementOrderId) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        if (enforcementOrder.getWarrantDetails() == null || hasWarrantDocument(enforcementOrder)) {
            return Optional.empty();
        }
        return Optional.of(toPayload(enforcementOrder));
    }

    @Transactional
    public void attachDocument(UUID enforcementOrderId, String documentUrl) {
        EnforcementOrderEntity enforcementOrder = loadEnforcementOrder(enforcementOrderId);
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);

        DocumentEntity document = documentImportService.addDocumentToCase(
            pcsCase, documentUrl, CaseFileCategory.ENFORCEMENT);
        document.setType(DocumentType.WARRANT_OF_POSSESSION);
        enforcementOrder.addDocuments(List.of(document));

        claimActivityLogService.logGenerationSuccess(pcsCase, claimant);
    }

    private WarrantOfPossessionPayload toPayload(EnforcementOrderEntity enforcementOrder) {
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();
        WarrantEntity warrant = enforcementOrder.getWarrantDetails();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);
        StatementOfTruthEntity statementOfTruth = enforcementOrder.getStatementOfTruth();

        return WarrantOfPossessionPayload.builder()
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
            // Application details
            .applicationType(APPLICATION_TYPE)
            .possessionOrderType(warrant.getIsSuspendedOrder() == VerticalYesNo.YES ? SUSPENDED_ORDER : OTHER_ORDER)
            // TODO: possession order date is not captured at the moment
            .possessionOrderDate(null)
            .breathingSpaceAnswer(warrant.getDefendantInBreathingSpace() == null
                                      ? null : warrant.getDefendantInBreathingSpace().getLabel())
            .missedPaymentsAnswer(warrant.getDefendantMissedPayments() == null
                                      ? null : warrant.getDefendantMissedPayments().getLabel())
            .propertyAddress(formatAddress(pcsCase.getPropertyAddress()))
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
        return defendantsToEvict(enforcementOrder).stream()
            .map(defendant -> EnforcementDefendant.builder()
                .name(partyService.getPartyName(defendant))
                .address(formatAddress(defenceCorrespondenceAddressResolver
                                           .resolveCorrespondenceAddress(defendant, propertyAddress)))
                .build())
            .toList();
    }

    /**
     * Every defendant on the claim when the claimant chose to evict everyone, otherwise the defendants they selected.
     */
    private List<PartyEntity> defendantsToEvict(EnforcementOrderEntity enforcementOrder) {
        if (enforcementOrder.getWarrantDetails().getEvictEveryone() == VerticalYesNo.YES) {
            return partiesByRole(enforcementOrder.getPcsCase().getMainClaim(), PartyRole.DEFENDANT);
        }
        return enforcementOrder.getSelectedDefendants().stream()
            .map(SelectedDefendantEntity::getParty)
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

    private boolean hasWarrantDocument(EnforcementOrderEntity enforcementOrder) {
        return enforcementOrder.getDocuments().stream()
            .anyMatch(document -> document.getType() == DocumentType.WARRANT_OF_POSSESSION);
    }

    private EnforcementOrderEntity loadEnforcementOrder(UUID enforcementOrderId) {
        return enforcementOrderRepository.findById(enforcementOrderId)
            .orElseThrow(() -> new EnforcementApplicationNotFoundException(
                "Unable to find Enforcement app with ID " + enforcementOrderId));
    }
}
