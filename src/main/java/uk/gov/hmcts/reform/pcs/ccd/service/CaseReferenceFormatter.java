package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;

@Service
public class CaseReferenceFormatter {

    public String formatCaseReferenceWithDashes(Long caseId) {
        if (caseId == null) {
            return null;
        }

        String unformattedCaseReference = String.format("%016d", caseId);
        return String.format(
            "%4s-%4s-%4s-%4s",
            unformattedCaseReference.substring(0, 4),
            unformattedCaseReference.substring(4, 8),
            unformattedCaseReference.substring(8, 12),
            unformattedCaseReference.substring(12, 16)
        );
    }

}
