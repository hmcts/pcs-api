package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.AdditionalPayment;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.Occupant;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.PaymentLine;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.Row;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails.Section;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SubmissionChannel;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimOccupantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimPaymentHistoryLineEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageAdditionalPaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageParticularsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.BulkClaimDetailsRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/** The bulk claim details tab: PCOL data carried by claims created through the bulk API. */
@Component
@RequiredArgsConstructor
public class BulkClaimDetailsView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    private final BulkClaimSubmissionRepository bulkClaimSubmissionRepository;
    private final BulkClaimDetailsRenderer renderer;
    private final MoneyFormatter moneyFormatter;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        ClaimEntity claim = pcsCaseEntity.getMainClaim();
        if (claim == null || claim.getSubmissionChannel() != SubmissionChannel.BULK_API) {
            pcsCase.setBulkClaimDetailsMarkdown(null);
            return;
        }

        BulkClaimDetails details = new BulkClaimDetails(
            submissionRows(claim, pcsCaseEntity.getCaseReference()),
            partySections(claim),
            paymentHistory(claim),
            mortgageRows(claim.getMortgageParticulars()),
            additionalPayments(claim.getMortgageParticulars()),
            originalProviderRows(claim.getMortgageParticulars()),
            occupants(claim)
        );
        if (details.isEmpty()) {
            pcsCase.setBulkClaimDetailsMarkdown(null);
            return;
        }
        pcsCase.setBulkClaimDetailsMarkdown(renderer.render(details));
    }

    private List<Row> submissionRows(ClaimEntity claim, Long caseReference) {
        Optional<BulkClaimSubmissionEntity> submission = caseReference == null
            ? Optional.empty()
            : bulkClaimSubmissionRepository.findByCaseReference(caseReference);

        List<Row> rows = new ArrayList<>();
        add(rows, "Customer unique identifier",
            submission.map(BulkClaimSubmissionEntity::getCustomerUniqueIdentifier).orElse(null));
        add(rows, "Schema version", submission.map(BulkClaimSubmissionEntity::getSchemaVersion).orElse(null));
        add(rows, "PCOL claimant reference", claim.getLegacyPcolClaimantReference());
        add(rows, "PCOL solicitor reference", claim.getLegacyPcolSolicitorReference());
        add(rows, "Claimant designation", claim.getClaimantDesignation());
        add(rows, "Solicitor firm", claim.getSolicitorFirmName());
        add(rows, "Claiming costs", yesNo(claim.getClaimCosts()));
        return rows;
    }

    private List<Section> partySections(ClaimEntity claim) {
        return Stream.of(PartyRole.CLAIMANT, PartyRole.DEFENDANT)
            .flatMap(role -> claim.getClaimParties().stream()
                .filter(claimParty -> claimParty.getRole() == role)
                .map(ClaimPartyEntity::getParty)
                .map(party -> partySection(role, party)))
            .filter(section -> !section.rows().isEmpty())
            .toList();
    }

    private Section partySection(PartyRole role, PartyEntity party) {
        List<Row> rows = new ArrayList<>();
        add(rows, "Title", party.getTitle());
        add(rows, "Initials", party.getInitials());
        add(rows, "Other names", party.getOtherNames());
        add(rows, "DX number", party.getDxNumber());
        add(rows, "Fax number", party.getFaxNumber());
        add(rows, "Telephone number", party.getPhoneNumber());
        add(rows, "Email address", party.getEmailAddress());
        add(rows, "Occupant", yesNo(party.getIsOccupant()));
        String heading = role == PartyRole.CLAIMANT ? "Claimant" : "Defendant";
        return new Section(heading + ": " + partyName(party), rows);
    }

    private String partyName(PartyEntity party) {
        String name = Stream.of(party.getFirstName(), party.getLastName())
            .filter(part -> part != null && !part.isBlank())
            .reduce((first, second) -> first + " " + second)
            .orElse(party.getOrgName());
        return name == null ? "" : name;
    }

    private List<PaymentLine> paymentHistory(ClaimEntity claim) {
        return claim.getPaymentHistoryLines().stream()
            .sorted(Comparator.comparing(ClaimPaymentHistoryLineEntity::getRank,
                                                   Comparator.nullsLast(Integer::compareTo)))
            .map(line -> new PaymentLine(
                date(line.getPaymentDueDate()),
                money(line.getAmountDue()),
                money(line.getAmountPaid()),
                money(line.getCumulativeArrears())))
            .toList();
    }

    private List<Row> mortgageRows(MortgageParticularsEntity mortgage) {
        List<Row> rows = new ArrayList<>();
        if (mortgage == null) {
            return rows;
        }
        add(rows, "Mortgage date", date(mortgage.getMortgageDate()));
        add(rows, "Default notice date", date(mortgage.getDefaultNoticeDate()));
        add(rows, "Loan amount", money(mortgage.getLoanAmount()));
        add(rows, "Current repayment terms", mortgage.getCurrentRepaymentTerms());
        add(rows, "Full repayment date", date(mortgage.getFullRepaymentDate()));
        add(rows, "Full repayment amount", money(mortgage.getFullRepaymentAmount()));
        add(rows, "Costs and administration", money(mortgage.getCostsAndAdministration()));
        add(rows, "Outstanding amount", money(mortgage.getOutstandingAmount()));
        add(rows, "Start interest rate", rate(mortgage.getStartInterestRate()));
        add(rows, "Pre-arrears interest rate", rate(mortgage.getPreArrearsInterestRate()));
        add(rows, "Claim start interest rate", rate(mortgage.getClaimStartInterestRate()));
        add(rows, "Interested party", mortgage.getInterestedParty());
        add(rows, "Tenancy date", date(mortgage.getTenancyDate()));
        add(rows, "Tenant notice served date", date(mortgage.getTenantNoticeServedDate()));
        add(rows, "Total arrears", money(mortgage.getTotalArrears()));
        add(rows, "Recovery steps", mortgage.getRecoverySteps());
        add(rows, "Money judgment wanted", yesNo(mortgage.getMoneyJudgmentWanted()));
        return rows;
    }

    private List<AdditionalPayment> additionalPayments(MortgageParticularsEntity mortgage) {
        if (mortgage == null) {
            return List.of();
        }
        return mortgage.getAdditionalPayments().stream()
            .sorted(Comparator.comparing(MortgageAdditionalPaymentEntity::getRank,
                                                   Comparator.nullsLast(Integer::compareTo)))
            .map(payment -> new AdditionalPayment(
                money(payment.getAmount()), nullToEmpty(payment.getReason()), money(payment.getArrears())))
            .toList();
    }

    private List<Row> originalProviderRows(MortgageParticularsEntity mortgage) {
        List<Row> rows = new ArrayList<>();
        if (mortgage == null) {
            return rows;
        }
        add(rows, "Name", mortgage.getOriginalProviderName());
        add(rows, "Date bought", date(mortgage.getOriginalProviderBoughtDate()));
        add(rows, "Date defendant notified", date(mortgage.getOriginalProviderDefendantNotifiedDate()));
        return rows;
    }

    private List<Occupant> occupants(ClaimEntity claim) {
        return claim.getOccupants().stream()
            .sorted(Comparator.comparing(ClaimOccupantEntity::getRank,
                                                   Comparator.nullsLast(Integer::compareTo)))
            .map(occupant -> new Occupant(
                occupantName(occupant), nullToEmpty(occupant.getOtherNames()), nullToEmpty(occupant.getInitials())))
            .toList();
    }

    private String occupantName(ClaimOccupantEntity occupant) {
        return Stream.of(occupant.getTitle(), occupant.getFirstName(), occupant.getLastName())
            .filter(part -> part != null && !part.isBlank())
            .reduce((first, second) -> first + " " + second)
            .orElse("");
    }

    private static void add(List<Row> rows, String label, String value) {
        if (value != null && !value.isBlank()) {
            rows.add(new Row(label, value));
        }
    }

    private String money(BigDecimal amount) {
        return amount == null ? "" : moneyFormatter.formatFee(amount);
    }

    private static String rate(BigDecimal rate) {
        return rate == null ? null : rate.stripTrailingZeros().toPlainString() + "%";
    }

    private static String date(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMAT);
    }

    private static String yesNo(YesOrNo value) {
        if (value == null) {
            return null;
        }
        return value == YesOrNo.YES ? "Yes" : "No";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
