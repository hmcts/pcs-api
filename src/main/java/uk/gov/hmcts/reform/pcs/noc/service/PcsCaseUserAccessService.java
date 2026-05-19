package uk.gov.hmcts.reform.pcs.noc.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobEntity;
import uk.gov.hmcts.reform.pcs.noc.model.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.pcs.noc.model.CaseUserRolesRequest;

@Service
@AllArgsConstructor
public class PcsCaseUserAccessService {

    private final CcdCaseUserApi ccdCaseUserApi;
    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;

    public void grantCaseRole(NocSideEffectJobEntity job) {
        ccdCaseUserApi.addCaseUserRoles(
            idamService.getSystemUserAuthorisation(),
            authTokenGenerator.generate(),
            requestFor(job)
        );
    }

    public void revokeCaseRole(NocSideEffectJobEntity job) {
        ccdCaseUserApi.removeCaseUserRoles(
            idamService.getSystemUserAuthorisation(),
            authTokenGenerator.generate(),
            requestFor(job)
        );
    }

    private CaseUserRolesRequest requestFor(NocSideEffectJobEntity job) {
        return new CaseUserRolesRequest(List.of(new CaseUserRoleWithOrganisation(
            String.valueOf(job.getCaseReference()),
            job.getUserId(),
            job.getCaseRole(),
            job.getOrganisationId()
        )));
    }
}
