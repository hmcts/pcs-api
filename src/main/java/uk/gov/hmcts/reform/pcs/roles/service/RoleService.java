package uk.gov.hmcts.reform.pcs.roles.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

//    private final UserInfoService userInfoService;
//    private final AuthTokenGenerator authTokenGenerator;
//    private final RoleAssignmentServiceApi roleAssignmentServiceApi;
//    private final HttpServletRequest httpServletRequest;
//
//    public RoleService(UserInfoService userInfoService,
//                       @Qualifier("rolesServiceAuthTokenGenerator") AuthTokenGenerator authTokenGenerator,
//                       RoleAssignmentServiceApi roleAssignmentServiceApi,
//                       HttpServletRequest httpServletRequest) {
//
//        this.userInfoService = userInfoService;
//        this.authTokenGenerator = authTokenGenerator;
//        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
//        this.httpServletRequest = httpServletRequest;
//    }
//
//    public List<String> getRolesForCurrentUser(long caseReference) {
//        String authorisation = httpServletRequest.getHeader(AUTHORIZATION);
//        UserInfo currentUserInfo = userInfoService.getCurrentUserInfo();
//        String actorId = currentUserInfo.getUid();
//
//        // TODO: Add short lived local cache here (or use Redis?)
//
//        String s2sToken = authTokenGenerator.generate();
//        GetRoleAssignmentResponseWrapper response = roleAssignmentServiceApi.getRoleAssignments(
//            authorisation,
//            s2sToken,
//            actorId
//        );
//
//        // TODO: Handle errors
//        // TODO: Filter by roleType, grantType, roleCategory etc?
//        List<String> roles = response.getRoleAssignments().stream()
//            .filter(roleAssignment -> isApplicable(roleAssignment, caseReference))
//            .map(RoleAssignment::getRoleName)
//            .toList();
//
//        logger.debug("Applicable roles for actor {} are {}", actorId, roles);
//
//        return roles;
//    }
//
//    public void assignCaseRoleToCurrentUser(UserRole userRole, long caseReference) {
//        String authorisation = httpServletRequest.getHeader(AUTHORIZATION);
//        UserInfo currentUserInfo = userInfoService.getCurrentUserInfo();
//        String actorId = currentUserInfo.getUid();
//        RoleAssignmentRequest roleAssignmentRequest = createRoleAssignmentRequest(actorId, userRole, caseReference);
//
//        String s2sToken = authTokenGenerator.generate();
//        roleAssignmentServiceApi.createRoleAssignments(
//            authorisation,
//            s2sToken,
//            roleAssignmentRequest
//        );
//    }
//
//    private static RoleAssignmentRequest createRoleAssignmentRequest(String actorId,
//                                                                     UserRole userRole,
//                                                                     long caseReference) {
//
//        RoleRequest roleRequest = RoleRequest.builder()
//            .assignerId(actorId)
//            .requestType(RequestType.CREATE)
//            .process("pcs-api")
//            .reference("roles-poc")
//            .build();
//
//        RoleAssignment roleAssignment = RoleAssignment.builder()
//            .actorId(actorId)
//            .actorIdType(ActorIdType.IDAM)
//            .roleType(RoleType.CASE)
//            .roleName(userRole.getRole())
//            .classification(Classification.RESTRICTED)
//            .roleCategory(RoleCategory.PROFESSIONAL)
//            .grantType(GrantType.SPECIFIC)
//            .attributes(RoleAssignmentAttributes.builder()
//                            .jurisdiction(CaseType.JURISDICTION_ID)
//                            .caseType(CaseType.CASE_TYPE_ID)
//                            .caseId(Long.toString(caseReference))
//                            .build())
//            .build();
//
//        return RoleAssignmentRequest.builder()
//            .roleRequest(roleRequest)
//            .requestedRoles(List.of(roleAssignment))
//            .build();
//    }
//
//    private boolean isApplicable(RoleAssignment roleAssignment, long caseReference) {
//        RoleAssignmentAttributes attributes = roleAssignment.getAttributes();
//
//        return isApplicableForCaseReference(roleAssignment, caseReference)
//            && isWithinDateRange(roleAssignment)
//            && isApplicableForCaseType(attributes)
//            && isApplicableForJurisdiction(attributes);
//    }
//
//    private boolean isApplicableForCaseReference(RoleAssignment roleAssignment, long caseReference) {
//        String caseReferenceString = Long.toString(caseReference);
//
//        return Optional.ofNullable(roleAssignment.getAttributes().getCaseId())
//            .map(caseReferenceString::equals)
//            .orElse(true);
//    }
//
//    private boolean isWithinDateRange(RoleAssignment roleAssignment) {
//        Instant now = Instant.now();
//        Instant beginTime = roleAssignment.getBeginTime();
//        Instant endTime = roleAssignment.getEndTime();
//
//        return (beginTime == null || now.isAfter(beginTime))
//            && (endTime == null || now.isBefore(endTime));
//    }
//
//    private boolean isApplicableForCaseType(RoleAssignmentAttributes attributes) {
//        return CaseType.CASE_TYPE_ID.equals(attributes.getCaseType());
//    }
//
//    private boolean isApplicableForJurisdiction(RoleAssignmentAttributes attributes) {
//        return CaseType.JURISDICTION_ID.equals(attributes.getJurisdiction());
//    }

}
