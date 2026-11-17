package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

/**
 * Who can see documents in the Enforcement folder: staff, judiciary and claimants.
 */
@Service
public class EnforcementDocumentVisibilityService {

    private static final Set<DocumentType> ENFORCEMENT_DOCUMENT_TYPES = EnumSet.of(
        DocumentType.WRIT_OF_POSSESSION, DocumentType.WARRANT_OF_POSSESSION, DocumentType.RISK_ASSESSMENT);

    private static final Set<String> VISIBLE_TO_ROLES = Stream.of(
            Arrays.stream(CASEWORKER_ROLES),
            Arrays.stream(JUDICIAL_HISTORY_ROLES),
            Stream.of(UserRole.CLAIMANT_SOLICITOR, UserRole.CLAIMANT))
        .flatMap(roles -> roles)
        .map(UserRole::getRole)
        .collect(Collectors.toUnmodifiableSet());

    public boolean isEnforcementDocument(DocumentType documentType) {
        return ENFORCEMENT_DOCUMENT_TYPES.contains(documentType);
    }

    public boolean isVisibleToUser(Collection<String> userRoles) {
        return userRoles.stream().anyMatch(VISIBLE_TO_ROLES::contains);
    }
}
