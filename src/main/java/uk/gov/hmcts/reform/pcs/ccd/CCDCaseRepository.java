package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.HearingFee;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

import java.math.BigDecimal;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PCSCaseRepository repository;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseRef The case to load
     */
    @Override
    public PCSCase getCase(long caseRef, String roleAssignments) {
        // Load the case from our database.
        var pcsCase = repository.getReferenceById(caseRef);
        // Translate it into the CCD model.
        boolean documentsProvided = BooleanUtils.isTrue(pcsCase.getDocumentsProvided());

        HearingFee hearingFee = HearingFee.builder()
            .dueDate(pcsCase.getFeeDueDate())
            .amount(convertToPenceString(pcsCase.getFeeAmount()))
            .paid(YesOrNo.from(BooleanUtils.isTrue(pcsCase.getFeePaid())))
            .build();

        return
            PCSCase.builder()
                .caseDescription(pcsCase.getCaseDescription())
                .documentsProvided(YesOrNo.from(documentsProvided))
                .hearingDate(pcsCase.getHearingDate() != null ? pcsCase.getHearingDate().toLocalDate() : null)
                .hearingFee(hearingFee)
                .build();
    }

    private static String convertToPenceString(BigDecimal amountInPounds) {
        return amountInPounds != null ? amountInPounds.movePointRight(2).toPlainString() : null;
    }

}
