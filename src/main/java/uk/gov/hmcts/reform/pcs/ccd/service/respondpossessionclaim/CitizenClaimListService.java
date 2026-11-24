package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimantClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CitizenClaimListService {

    private final PartyRepository partyRepository;
    private final CoreCaseDataApi coreCaseDataApi;
    private final AuthTokenGenerator authTokenGenerator;

    public List<ClaimSummary> getClaimsAgainst(String idamId) {
        return partyRepository.findClaimsByIdamIdAndRole(idamId, PartyRole.DEFENDANT).stream()
            .map(claim -> ClaimSummary.builder()
                .caseReference(String.valueOf(claim.getPcsCase().getCaseReference()))
                .claimantName(extractClaimantName(claim))
                .propertyPostcode(extractPostcode(claim))
                .build())
            .toList();
    }

    /**
     * The cases the citizen created as claimant, newest first. Each is read from CCD with the citizen's own token,
     * which gives the case state and confirms they hold [CREATOR] on it; a case CCD will not show them is left out.
     */
    public List<ClaimantClaimSummary> getClaimsMadeBy(String idamId, String authorisation) {
        return partyRepository.findCasesCreatedBy(idamId).stream()
            .sorted(Comparator.comparing(PcsCaseEntity::getCaseReference).reversed())
            .map(pcsCase -> toClaimantSummary(pcsCase, authorisation))
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<ClaimantClaimSummary> toClaimantSummary(PcsCaseEntity pcsCase, String authorisation) {
        String caseReference = String.valueOf(pcsCase.getCaseReference());
        try {
            CaseDetails caseDetails = coreCaseDataApi.getCase(
                authorisation, authTokenGenerator.generate(), caseReference);
            return Optional.of(ClaimantClaimSummary.builder()
                .caseReference(caseReference)
                .state(caseDetails.getState())
                .propertyAddress(toPropertyAddress(pcsCase.getPropertyAddress()))
                .build());
        } catch (FeignException.Forbidden | FeignException.NotFound e) {
            log.warn("Case {} is not readable by the claimant who created it", caseReference);
            return Optional.empty();
        }
    }

    private ClaimantClaimSummary.PropertyAddress toPropertyAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        return ClaimantClaimSummary.PropertyAddress.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .addressLine3(address.getAddressLine3())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .postcode(address.getPostcode())
            .build();
    }

    private String extractClaimantName(ClaimEntity claim) {
        return claim.getClaimParties().stream()
            .filter(cp -> cp.getRole() == PartyRole.CLAIMANT)
            .map(ClaimPartyEntity::getParty)
            .map(claimant -> (claimant.getOrgName() != null
                ? claimant.getOrgName()
                : String.format("%s %s", claimant.getFirstName(), claimant.getLastName())))
            .findFirst()
            .orElse(null);
    }

    private String extractPostcode(ClaimEntity claim) {
        return Optional.ofNullable(claim.getPcsCase().getPropertyAddress())
            .map(AddressEntity::getPostcode)
            .orElse(null);
    }
}
