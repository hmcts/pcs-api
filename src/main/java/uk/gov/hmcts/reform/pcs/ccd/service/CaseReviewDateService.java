package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseReviewDateEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@Service
@AllArgsConstructor
public class CaseReviewDateService {

    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    @Qualifier("ukClock")
    private final Clock ukClock;
    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;

    public void addCaseReviewDates(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<ListValue<ReviewDate>> reviewDates = pcsCase.getReviewDates();
        String waReviewDueDateTaskDescription = taskDescriptionService.createReviewDueDateDescription(caseReference);
        for (ListValue<ReviewDate> listValue : reviewDates) {
            ReviewDate reviewDate = listValue.getValue();
            CaseReviewDateEntity caseReviewDateEntity = createCaseReviewDateEntity(reviewDate);
            pcsCaseEntity.addCaseReviewDate(caseReviewDateEntity);

            Instant waTaskCreationDate = reviewDate.getDate().atStartOfDay().atZone(UK_ZONE_ID).toInstant();
            camundaService.createTask(
                caseReference,
                TaskType.REVIEW_DATE_DUE,
                waReviewDueDateTaskDescription,
                waTaskCreationDate
            );
        }
        pcsCaseRepository.save(pcsCaseEntity);
    }

    private CaseReviewDateEntity createCaseReviewDateEntity(ReviewDate reviewDate) {
        UserInfo userInfo = securityContextService.getCurrentUserDetails();

        return CaseReviewDateEntity.builder()
            .createdBy(userInfo.getName())
            .createdDate(LocalDateTime.now(ukClock))
            .date(reviewDate.getDate())
            .reason(reviewDate.getReason())
            .description(reviewDate.getDescription())
            .build();
    }
}
