package uk.gov.hmcts.reform.pcs.ccd.service.writofpossession;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.writofpossession.WritOfPossessionPayload;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.partiesByRole;

@Service
@AllArgsConstructor
public class WritOfPossessionPayloadBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
    static final String COMPLETED_BY_CLAIMANT = "Claimant";
    static final String COMPLETED_BY_LEGAL_REPRESENTATIVE = "Claimant’s legal representative";
    static final String HCEO_SELECTED_BY_NICE = "The National Information Centre for Enforcement will choose a "
        + "High Court Enforcement Officer for you";

    private final PartyService partyService;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final RecipientAddressResolver recipientAddressResolver;

    public WritOfPossessionPayload build(EnforcementOrderEntity enforcementOrder) {
        PcsCaseEntity pcsCase = enforcementOrder.getPcsCase();
        WritEntity writ = enforcementOrder.getWritDetails();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);
        String propertyAddress = formatAddress(pcsCase.getPropertyAddress());
        StatementOfTruthEntity statementOfTruth = enforcementOrder.getStatementOfTruth();

        return WritOfPossessionPayload.builder()
            .referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()))
            .caseName(caseNameFormatter.formatCaseName(pcsCase))
            .claimantName(partyService.getPartyName(claimant))
            .claimantAddress(formatAddress(recipientAddressResolver.resolvePostalAddress(
                claimant, PartyRole.CLAIMANT, pcsCase.getPropertyAddress())))
            .claimantTelephoneNumber(claimant.getPhoneNumber())
            .claimantEmailAddress(claimant.getEmailAddress())
            .defendantNames(joinDefendantNames(enforcementOrder))
            .propertyAddress(propertyAddress)
            .breathingSpaceAnswer(writ.getDefendantInBreathingSpace() == null
                                      ? null : writ.getDefendantInBreathingSpace().getLabel())
            .highCourtPermissionAnswer(writ.getHasClaimTransferredToHighCourt() == null
                                           ? null : writ.getHasClaimTransferredToHighCourt().getValue())
            .hceoHiredAnswer(writ.getHasHiredHighCourtEnforcementOfficer() == null
                                 ? null : writ.getHasHiredHighCourtEnforcementOfficer().getLabel())
            .hceoName(writ.getHasHiredHighCourtEnforcementOfficer() == VerticalYesNo.YES
                          ? writ.getHceoDetails() : null)
            .hceoSelectionOutcome(writ.getHasHiredHighCourtEnforcementOfficer() == VerticalYesNo.NO
                                      ? HCEO_SELECTED_BY_NICE : null)
            .statementOfTruthCompletedBy(completedBy(statementOfTruth.getCompletedBy()))
            .signatoryFullName(statementOfTruth.getFullName())
            .signatoryPosition(statementOfTruth.getPositionHeld())
            .signatoryOrganisation(statementOfTruth.getFirmName())
            .submissionDate(DATE_FORMATTER.format(statementOfTruth.getCompletedDate()))
            .build();
    }

    private String joinDefendantNames(EnforcementOrderEntity enforcementOrder) {
        return partiesByRole(enforcementOrder.getPcsCase().getMainClaim(), PartyRole.DEFENDANT).stream()
            .map(partyService::getPartyName)
            .collect(Collectors.joining(", "));
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
