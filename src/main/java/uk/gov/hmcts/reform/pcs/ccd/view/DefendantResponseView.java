package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DefendantResponseView {

    private final SecurityContextService securityContextService;
    private final DefendantAccessValidator accessValidator;
    private final DefendantResponseService defendantResponseService;
    private final AddressMapper addressMapper;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (!isCitizenUser()) {
            return;
        }

        long caseReference = pcsCaseEntity.getCaseReference();
        if (!defendantResponseService.hasSubmittedResponse(caseReference)) {
            return;
        }

        UUID userId = securityContextService.getCurrentUserId();
        accessValidator.validateAndGetDefendant(pcsCaseEntity, userId);

        PossessionClaimResponse response =
            defendantResponseService.getSubmittedResponse(caseReference);

        enrichClaimantServiceAddress(pcsCaseEntity, response);
        enrichClaimantOrganisations(pcsCaseEntity, response);
        pcsCase.setPossessionClaimResponse(response);
    }

    private boolean isCitizenUser() {
        return securityContextService.getCurrentUserDetails().getRoles()
            .contains(UserRole.CITIZEN.getRole());
    }

    private void enrichClaimantServiceAddress(PcsCaseEntity caseEntity, PossessionClaimResponse response) {
        findPrimaryClaimant(caseEntity).ifPresent(claimant -> {
            Optional.ofNullable(claimant.getAddress())
                .map(addressMapper::toAddressUK)
                .ifPresent(response::setClaimantServiceAddress);
        });
    }

    private void enrichClaimantOrganisations(PcsCaseEntity caseEntity, PossessionClaimResponse response) {
        findPrimaryClaimant(caseEntity).ifPresent(claimant -> {
            if (StringUtils.isNotBlank(claimant.getOrgName())) {
                response.setClaimantOrganisations(List.of(
                    ListValue.<String>builder()
                        .id(claimant.getId() != null ? claimant.getId().toString() : null)
                        .value(claimant.getOrgName())
                        .build()
                ));
            }
        });
    }

    // TODO(HDPI-5535): Replace with PartyService.getPrimaryClaimantPartyEntity once PR #1856 is merged.
    private static Optional<PartyEntity> findPrimaryClaimant(PcsCaseEntity caseEntity) {
        if (caseEntity.getClaims() == null || caseEntity.getClaims().isEmpty()) {
            return Optional.empty();
        }
        ClaimEntity claim = caseEntity.getClaims().getFirst();
        if (claim.getClaimParties() == null) {
            return Optional.empty();
        }
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT)
            .map(ClaimPartyEntity::getParty)
            .findFirst();
    }
}