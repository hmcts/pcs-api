package uk.gov.hmcts.reform.pcs.ccd.service.counterclaimform;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.document.model.counterclaimform.CounterClaimFormPayload;

import java.time.Clock;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.formatUkDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.toLabel;

@Service
@AllArgsConstructor
public class CounterClaimFormPayloadBuilder {
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    @Qualifier("ukClock")
    private final Clock ukClock;

    public CounterClaimFormPayload build(CounterClaimEntity counterClaim) {
        ClaimEntity mainClaim = counterClaim.getPcsCase().getClaims().stream()
            .findFirst()
            .orElse(null);

        String claimingFor = counterClaim.getClaimType() != null ? counterClaim.getClaimType().getLabel() : null;
        String claimingSpecificSum = toLabel(counterClaim.getIsClaimAmountKnown());
        String claimAmount = formatGbp(counterClaim.getClaimAmount());
        String maximumClaimValue = formatGbp(counterClaim.getEstimatedMaxClaimAmount());
        String needsHelpWithFees = toLabel(counterClaim.getNeedHelpWithFees());
        String hwfRef = counterClaim.getHwfReferenceNumber();
        String respondentNames = buildRespondentNames(counterClaim);
        String counterClaimFor = counterClaim.getCounterClaimFor();
        String counterClaimReasons = counterClaim.getCounterClaimReasons();
        String otherOrderDetails = counterClaim.getOtherOrderRequestDetails();
        String otherOrderFacts = counterClaim.getOtherOrderRequestFacts();
        String statementOfTruthName = buildStatementOfTruthName(counterClaim);
        boolean showOtherOrder = StringUtils.hasText(otherOrderDetails) || StringUtils.hasText(otherOrderFacts);

        return CounterClaimFormPayload.builder()
            .referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(
                counterClaim.getPcsCase().getCaseReference()))
            .caseName(buildCaseName(mainClaim))
            .issueDateSealed(formatUkDate(counterClaim.getClaimIssuedDate(), ukClock))
            .submittedOn(formatUkDate(counterClaim.getClaimSubmittedDate(), ukClock))
            .claimingFor(claimingFor)
            .claimingSpecificSum(claimingSpecificSum)
            .claimAmount(claimAmount)
            .maximumClaimValue(maximumClaimValue)
            .needsHelpWithFees(needsHelpWithFees)
            .hwfReferenceNumber(hwfRef)
            .respondentNames(respondentNames)
            .counterClaimFor(counterClaimFor)
            .counterClaimReasons(counterClaimReasons)
            .otherOrderRequestDetails(otherOrderDetails)
            .otherOrderRequestFacts(otherOrderFacts)
            .statementOfTruthName(statementOfTruthName)
            .showClaimingFor(StringUtils.hasText(claimingFor))
            .showClaimingSpecificSum(StringUtils.hasText(claimingSpecificSum))
            .showClaimAmount(StringUtils.hasText(claimAmount))
            .showMaximumClaimValue(StringUtils.hasText(maximumClaimValue))
            .showNeedsHelpWithFees(StringUtils.hasText(needsHelpWithFees))
            .showHwfRef(StringUtils.hasText(hwfRef))
            .showRespondentNames(StringUtils.hasText(respondentNames))
            .showCounterClaimFor(StringUtils.hasText(counterClaimFor))
            .showCounterClaimReasons(StringUtils.hasText(counterClaimReasons))
            .showOtherOrderSection(showOtherOrder)
            .showStatementOfTruthName(StringUtils.hasText(statementOfTruthName))
            .build();
    }

    private String buildCaseName(ClaimEntity mainClaim) {
        if (mainClaim == null) {
            return null;
        }
        List<PartyEntity> claimants = PartyDisplayMapper.partiesByRole(mainClaim, PartyRole.CLAIMANT);
        List<PartyEntity> defendants = PartyDisplayMapper.partiesByRole(mainClaim, PartyRole.DEFENDANT);
        return caseNameFormatter.formatCaseName(toDomainParties(claimants), toDomainParties(defendants));
    }

    private String buildRespondentNames(CounterClaimEntity counterClaim) {
        if (counterClaim.getCounterClaimParties().isEmpty()) {
            return null;
        }
        List<String> names = counterClaim.getCounterClaimParties().stream()
            .map(CounterClaimPartyEntity::getParty)
            .map(this::formatPartyDisplayName)
            .filter(StringUtils::hasText)
            .toList();
        return names.isEmpty() ? null : String.join("\n", names);
    }

    private String buildStatementOfTruthName(CounterClaimEntity counterClaim) {
        return counterClaim.findAssociatedDefendantResponse()
            .map(DefendantResponseEntity::getStatementOfTruth)
            .map(StatementOfTruthEntity::getFullName)
            .filter(StringUtils::hasText)
            .orElse(null);
    }

    private String formatPartyDisplayName(PartyEntity party) {
        if (party == null) {
            return null;
        }
        if (StringUtils.hasText(party.getOrgName())) {
            return party.getOrgName();
        }
        return PartyDisplayMapper.joinName(party.getFirstName(), party.getLastName());
    }

    private static List<uk.gov.hmcts.reform.pcs.ccd.domain.Party> toDomainParties(List<PartyEntity> parties) {
        return parties.stream().map(PartyDisplayMapper::toDomainParty).toList();
    }
}
