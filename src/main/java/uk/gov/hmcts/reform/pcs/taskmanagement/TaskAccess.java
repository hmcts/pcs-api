package uk.gov.hmcts.reform.pcs.taskmanagement;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.taskmanagement.model.TaskPermission;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.taskmanagement.RoleCategory.ADMIN;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskOperation.Claim;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskOperation.Complete;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskOperation.Manage;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskOperation.Own;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskOperation.Read;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskOperation.Unclaim;

@Getter
public enum TaskAccess {
    PCS_ADMIN(
        Set.of(Read,Own,Claim,Unclaim,Manage,Complete), ADMIN,
        false, 1,
        Authorisations.NONE),
    SYSTEM(
        Set.of(Read,Manage,Complete), ADMIN,
        false, 2,
        Authorisations.NONE);

    private final Set<TaskOperation> permissions;
    private final RoleCategory roleCategory;
    private final boolean autoAssignable;
    private final int assignmentPriority;
    private final Authorisations authorisations;

    TaskAccess(
        Set<TaskOperation> permissions,
        RoleCategory roleCategory,
        boolean autoAssignable,
        int assignmentPriority,
        Authorisations authorisations
    ) {
        this.permissions = permissions;
        this.roleCategory = roleCategory;
        this.autoAssignable = autoAssignable;
        this.assignmentPriority = assignmentPriority;
        this.authorisations = authorisations;
    }

    public TaskPermission toTaskPermission() {
        String roleName = name()
            .toLowerCase(Locale.UK)
            .replace("_specific_access", "")
            .replace('_', '-');
        List<String> authorisationsList = authorisations == Authorisations.NONE
            ? Collections.emptyList()
            : List.of(authorisations.getAuthorisation());

        return TaskPermission.builder()
            .roleName(roleName)
            .roleCategory(roleCategory.name())
            .permissions(permissions.stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .map(TaskOperation::name)
                .toList())
            .authorisations(authorisationsList)
            .assignmentPriority(assignmentPriority)
            .autoAssignable(autoAssignable)
            .build();
    }
}


