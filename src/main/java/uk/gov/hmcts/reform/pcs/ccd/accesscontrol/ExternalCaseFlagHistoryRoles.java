package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExternalCaseFlagHistoryRoles {

    public static final UserRole[] EXTERNAL_CASE_FLAG_HISTORY_ROLES = ArrayUtils.addAll(
        JUDICIAL_HISTORY_ROLES,
        UserRole.HEARING_CENTRE_ADMIN,
        UserRole.HEARING_CENTRE_TEAM_LEADER
    );

}
