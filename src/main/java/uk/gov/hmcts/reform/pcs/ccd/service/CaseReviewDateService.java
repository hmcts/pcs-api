package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class CaseReviewDateService {

    private final PcsCaseService pcsCaseService;
    private PcsCaseRepository pcsCaseRepository;

    public void addCaseReviewDate(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<ListValue<ReviewDate>> reviewDates = pcsCase.getReviewDates();
        for (ListValue<ReviewDate> listValue : reviewDates) {
            CaseReviewDateEntity caseReviewDateEntity = createCaseReviewDateEntity(listValue.getValue());
            pcsCaseEntity.addReviewDate(caseReviewDateEntity);
        }
        pcsCaseRepository.save(pcsCaseEntity);
    }

    private CaseReviewDateEntity createCaseReviewDateEntity(ReviewDate reviewDate) {
        return CaseReviewDateEntity.builder()
            .date(reviewDate.getDate())
            .reason(reviewDate.getReason())
            .description(reviewDate.getDescription())
            .build();
    }
}
