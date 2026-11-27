package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

/**
 * Who can see documents in the Enforcement folder: staff, judiciary and the claimant's organisation on the case.
 */
@Service
@RequiredArgsConstructor
public class EnforcementDocumentVisibilityService {

    private static final Set<DocumentType> ENFORCEMENT_DOCUMENT_TYPES = EnumSet.of(
        DocumentType.WRIT_OF_POSSESSION, DocumentType.WARRANT_OF_POSSESSION, DocumentType.RISK_ASSESSMENT);

    private static final Set<String> INTERNAL_ROLES = Stream.concat(
            Arrays.stream(CASEWORKER_ROLES),
            Arrays.stream(JUDICIAL_HISTORY_ROLES))
        .map(UserRole::getRole)
        .collect(Collectors.toUnmodifiableSet());

    private final PartyService partyService;

    public boolean isEnforcementDocument(DocumentType documentType) {
        return ENFORCEMENT_DOCUMENT_TYPES.contains(documentType);
    }

    /**
     * External users must belong to the claimant's organisation on this case.
     */
    public boolean isVisibleToUser(Collection<String> userRoles, PcsCaseEntity pcsCaseEntity, String organisationId) {
        if (userRoles.stream().anyMatch(INTERNAL_ROLES::contains)) {
            return true;
        }

        return organisationId != null
            && organisationId.equals(partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity).getOrganisationId());
    }
}
