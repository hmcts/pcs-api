package uk.gov.hmcts.reform.pcs.roles.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.roles.api.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.pcs.roles.model.RoleAssignment;
import uk.gov.hmcts.reform.pcs.roles.model.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.pcs.roles.model.RoleAssignmentResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.CaseType.PCS_CASE_TYPE;
import static uk.gov.hmcts.reform.pcs.ccd.CaseType.PCS_JURISDICTION;

@Service
public class RolesService {

    private static final Logger logger = LoggerFactory.getLogger(RolesService.class);

    private final AuthTokenGenerator authTokenGenerator;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    public RolesService(@Qualifier("rolesServiceAuthTokenGenerator") AuthTokenGenerator authTokenGenerator,
                        RoleAssignmentServiceApi roleAssignmentServiceApi) {
        this.authTokenGenerator = authTokenGenerator;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }

    public List<String> getRolesForActor(String authorisation, String actorId, long caseReference) {
        // TODO: Add short lived local cache here (or use Redis?)

        String s2sToken = authTokenGenerator.generate();
        RoleAssignmentResponse response = roleAssignmentServiceApi.getRoleAssignments(
            authorisation,
            s2sToken,
            actorId
        );

        // TODO: Handle errors
        // TODO: Filter by roleType, grantType, roleCategory etc?
        List<String> roles = response.getRoleAssignments().stream()
            .filter(roleAssignment -> isApplicable(roleAssignment, caseReference))
            .map(RoleAssignment::getRoleName)
            .toList();

        logger.debug("Applicable roles for actor {} are {}", actorId, roles);

        return roles;
    }

    private boolean isApplicable(RoleAssignment roleAssignment, long caseReference) {
        RoleAssignmentAttributes attributes = roleAssignment.getAttributes();

        return isApplicableForCaseReference(roleAssignment, caseReference)
            && isWithinDateRange(roleAssignment)
            && isApplicableForCaseType(attributes)
            && isApplicableForJurisdiction(attributes);
    }

    private boolean isApplicableForCaseReference(RoleAssignment roleAssignment, long caseReference) {
        String caseReferenceString = Long.toString(caseReference);

        return Optional.ofNullable(roleAssignment.getAttributes().getCaseId())
            .map(caseReferenceString::equals)
            .orElse(true);
    }

    private boolean isWithinDateRange(RoleAssignment roleAssignment) {
        Instant now = Instant.now();
        Instant beginTime = roleAssignment.getBeginTime();
        Instant endTime = roleAssignment.getEndTime();

        return (beginTime == null || now.isAfter(beginTime))
            && (endTime == null || now.isBefore(endTime));
    }

    private boolean isApplicableForCaseType(RoleAssignmentAttributes attributes) {
        return PCS_CASE_TYPE.equals(attributes.getCaseType());
    }

    private boolean isApplicableForJurisdiction(RoleAssignmentAttributes attributes) {
        return PCS_JURISDICTION.equals(attributes.getJurisdiction());
    }

}
