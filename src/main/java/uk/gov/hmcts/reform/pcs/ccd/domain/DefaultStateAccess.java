package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.CIVIL_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.MY_APPLICANT_ROLE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.MY_JUDGE_ROLE;


/**
 * Placeholder access control configuration granting caseworker CRU.
 */
public class DefaultStateAccess implements HasAccessControl {
    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
//        grants.putAll(CASE_WORKER_GENERIC, Set.of(Permission.R));
        grants.putAll(CIVIL_CASE_WORKER, Permission.CRU);
        grants.putAll(MY_APPLICANT_ROLE, Permission.CRU);
//        grants.putAll(MY_RESPONDENT_ROLE, Permission.CRU);
        grants.putAll(MY_JUDGE_ROLE, Permission.CRU);
        return grants;
    }
}
