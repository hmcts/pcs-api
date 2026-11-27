package uk.gov.hmcts.reform.pcs.ccd.service.warrantofpossession;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.warrantofpossession.WarrantOfPossessionPayload;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

@Service
@AllArgsConstructor
public class WarrantOfPossessionPayloadBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
    static final String SUSPENDED_ORDER = "Suspended order";
    static final String OTHER_ORDER = "Other";
    static final String COMPLETED_BY_CLAIMANT = "Claimant";
    static final String COMPLETED_BY_LEGAL_REPRESENTATIVE = "Claimant’s legal representative";

    private final PartyService partyService;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final RecipientAddressResolver recipientAddressResolver;

    public WarrantOfPossessionPayload build(EnforcementOrderEntity enforcementOrder) {
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();
        WarrantEntity warrant = enforcementOrder.getWarrantDetails();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);
        StatementOfTruthEntity statementOfTruth = enforcementOrder.getStatementOfTruth();

        return WarrantOfPossessionPayload.builder()
            .referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()))
            .caseName(caseNameFormatter.formatCaseName(pcsCase))
            .claimantName(partyService.getPartyName(claimant))
            .claimantAddress(formatAddress(recipientAddressResolver.resolvePostalAddress(
                claimant, PartyRole.CLAIMANT, pcsCase.getPropertyAddress())))
            .claimantTelephoneNumber(claimant.getPhoneNumber())
            .claimantEmailAddress(claimant.getEmailAddress())
            .defendantNames(joinDefendantNames(enforcementOrder))
            .applicationType(SelectEnforcementType.WARRANT.getLabel())
            .possessionOrderType(warrant.getIsSuspendedOrder() == VerticalYesNo.YES ? SUSPENDED_ORDER : OTHER_ORDER)
            .breathingSpaceAnswer(warrant.getDefendantInBreathingSpace() == null
                                      ? null : warrant.getDefendantInBreathingSpace().getLabel())
            .missedPaymentsAnswer(warrant.getDefendantMissedPayments() == null
                                      ? null : warrant.getDefendantMissedPayments().getLabel())
            .propertyAddress(formatAddress(pcsCase.getPropertyAddress()))
            .statementOfTruthCompletedBy(completedBy(statementOfTruth.getCompletedBy()))
            .signatoryFullName(statementOfTruth.getFullName())
            .signatoryPosition(statementOfTruth.getPositionHeld())
            .signatoryOrganisation(statementOfTruth.getFirmName())
            .submissionDate(DATE_FORMATTER.format(statementOfTruth.getCompletedDate()))
            .build();
    }

    private String joinDefendantNames(EnforcementOrderEntity enforcementOrder) {
        return defendantsToEvict(enforcementOrder).stream()
            .map(partyService::getPartyName)
            .collect(Collectors.joining(", "));
    }

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
        return addressFormatter.formatFullAddress(addressMapper.toAddressUK(address),
            AddressFormatter.NEWLINE_DELIMITER);
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
}
