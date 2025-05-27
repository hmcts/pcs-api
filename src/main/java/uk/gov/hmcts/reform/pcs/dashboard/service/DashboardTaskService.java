package uk.gov.hmcts.reform.pcs.dashboard.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.dashboard.model.Task;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DashboardTaskService {

    private static final long CASE_REF_WITH_NO_TASKS = 8888L;
    private static final long UNKNOWN_CASE_REF = 9999L;

    /**
     * Returns the active tasks for the specified case reference.
     *
     * @param caseReference The CCD case reference
     * @return Some mock tasks for now
     * @throws CaseNotFoundException - if the case cannot be found
     */
    public List<TaskGroup> getTasks(long caseReference) {


        if (caseReference == UNKNOWN_CASE_REF) {
            throw new CaseNotFoundException(caseReference);
        }

        if (caseReference == CASE_REF_WITH_NO_TASKS) {
            return List.of();
        }

        // CLAIM Task Group
        var claimGroup = TaskGroup.builder()
            .groupId("CLAIM")
            .tasks(List.of(
                Task.builder()
                    .templateId("Task.AAA6.Claim.ViewClaim")
                    .templateValues(Map.of())
                    .status("AVAILABLE")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Claim.Adjustments")
                    .templateValues(Map.of())
                    .status("AVAILABLE")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Claim.Equality")
                    .templateValues(Map.of())
                    .status("AVAILABLE")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Claim.Information")
                    .templateValues(Map.of())
                    .status("AVAILABLE")
                    .build()
            ))
            .build();

        // RESPONSE Task Group
        var responseGroup = TaskGroup.builder()
            .groupId("RESPONSE")
            .tasks(List.of(
                Task.builder()
                    .templateId("Task.AAA6.Response.ViewResponse")
                    .templateValues(Map.of())
                    .status("AVAILABLE")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Response.Information")
                    .templateValues(Map.of())
                    .status("COMPLETED")
                    .build()
            ))
            .build();

        // HEARING Task Group
        var hearingGroup = TaskGroup.builder()
            .groupId("HEARING")
            .tasks(List.of(
                Task.builder()
                    .templateId("Task.AAA6.Hearing.ViewHearing")
                    .templateValues(Map.of())
                    .status("AVAILABLE")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Hearing.UploadDocuments")
                    .templateValues(Map.of(
                        "deadline", ZonedDateTime.of(LocalDate.of(2025, 5, 20), LocalTime.of(15, 0, 0), ZoneId.of("UTC"))
                        
                    ))
                    .status("ACTION_NEEDED")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Hearing.ViewDocuments")
                    .templateValues(Map.of())
                    .status("NOT_AVAILABLE")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Hearing.TrialArrangments")
                    .templateValues(Map.of())
                    .status("NOT_AVAILABLE")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Hearing.PayFee")
                    .templateValues(Map.of(
                        "deadline", ZonedDateTime.of(LocalDate.of(2025, 6, 28), LocalTime.of(15, 0, 0), ZoneId.of("UTC"))
                    ))
                    .status("ACTION_NEEDED")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Hearing.ViewBundle")
                    .templateValues(Map.of())
                    .status("NOT_AVAILABLE")
                    .build()
            ))
            .build();

        // NOTICE Task Group
        var noticeGroup = TaskGroup.builder()
            .groupId("NOTICE")
            .tasks(List.of(
                Task.builder()
                    .templateId("Task.AAA6.Notice.ViewNotices")
                    .templateValues(Map.of())
                    .status("AVAILABLE")
                    .build()
            ))
            .build();

        // JUDGEMENT Task Group
        var judgementGroup = TaskGroup.builder()
            .groupId("JUDGEMENT")
            .tasks(List.of(
                Task.builder()
                    .templateId("Task.AAA6.Judgement.ViewJudgement")
                    .templateValues(Map.of())
                    .status("NOT_AVAILABLE")
                    .build()
            ))
            .build();

        // APPLICATIONS Task Group
        var applicationsGroup = TaskGroup.builder()
            .groupId("APPLICATIONS")
            .tasks(List.of(
                Task.builder()
                    .templateId("Task.AAA6.Applications.Contact")
                    .templateValues(Map.of())
                    .status("OPTIONAL")
                    .build(),
                Task.builder()
                    .templateId("Task.AAA6.Applications.ViewApplications")
                    .templateValues(Map.of())
                    .status("IN_PROGRESS")
                    .build()
            ))
            .build();

        return List.of(
            claimGroup, 
            responseGroup, 
            hearingGroup, 
            noticeGroup, 
            judgementGroup, 
            applicationsGroup
        );
    }
}
