package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.MY_APPLICANT_CASE_ROLE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.MY_APPLICANT_ROLE;


public class ApplicantAccess implements HasAccessControl {

    @Override
    public SetMultimap<HasRole, Permission> getGrants() {
        SetMultimap<HasRole, Permission> grants = HashMultimap.create();
        grants.putAll(MY_APPLICANT_CASE_ROLE, Permission.CRU);
        grants.putAll(MY_APPLICANT_ROLE, Set.of());
        return grants;
    }

}
