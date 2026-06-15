package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class PcsCaseService {

    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimService claimService;
    private final PartyService partyService;
    private final DocumentService documentService;
    private final TenancyLicenceService tenancyLicenceService;
    private final AddressMapper addressMapper;
    private final CaseLinkService caseLinkService;
    private final CaseFlagService caseFlagService;
    private final PostCodeCourtService postCodeCourtService;
    private final LocationReferenceService locationReferenceService;

    public PcsCaseEntity createCase(long caseReference,
                                    AddressUK propertyAddress,
                                    LegislativeCountry legislativeCountry) {

        Objects.requireNonNull(propertyAddress, "Property address must be provided to create a case");
        Objects.requireNonNull(legislativeCountry, "Legislative country must be provided to create a case");

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(addressMapper.toAddressEntityAndNormalise(propertyAddress));
        pcsCaseEntity.setLegislativeCountry(legislativeCountry);

        return pcsCaseRepository.save(pcsCaseEntity);
    }

    public void createMainClaimOnCase(long caseReference, PCSCase pcsCase, String organisationIdForCurrentUser) {
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        ClaimEntity claimEntity = claimService.createMainClaimEntity(pcsCase);
        List<DocumentEntity> documentEntities = documentService.createAllDocuments(pcsCase);
        documentEntities.forEach(doc -> doc.setClaim(claimEntity));
        pcsCaseEntity.addDocuments(documentEntities);
        claimEntity.addClaimDocuments(documentEntities);
        pcsCaseEntity.addClaim(claimEntity);
        pcsCaseEntity.setBaseLocation(pcsCase.getCaseManagementLocationNumber());
        pcsCaseEntity.setRegionId(pcsCase.getRegionId());

        partyService.createAllParties(pcsCase, pcsCaseEntity, claimEntity, organisationIdForCurrentUser);

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.createTenancyLicenceEntity(pcsCase));
        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void patchCaseFlags(long caseReference, PCSCase pcsCase) {
        if (pcsCase == null) {
            throw new IllegalArgumentException("PCSCase cannot be null");
        }
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        if (pcsCase.getCaseFlags() != null && pcsCase.getCaseFlags().getDetails() != null) {
            List<CaseFlagEntity> mergedFlagDetails = caseFlagService.mergeCaseFlags(pcsCase.getCaseFlags(),
                pcsCaseEntity);

            pcsCaseEntity.getCaseFlags().clear();
            pcsCaseEntity.getCaseFlags().addAll(mergedFlagDetails);
        }

        if (pcsCase.getParties() != null) {
            caseFlagService.mergePartyFlags(pcsCase.getParties(), pcsCaseEntity.getParties());
        }
    }

    public PcsCaseEntity loadCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    public void allocateCaseManagementLocation(PCSCase pcsCase) {
        Integer caseManagementLocation =
            postCodeCourtService.getCourtManagementLocation(pcsCase.getPropertyAddress().getPostCode(),
                                                            pcsCase.getLegislativeCountry());
        log.info("Setting caseManagementLocationNumber to: {}", caseManagementLocation);
        pcsCase.setCaseManagementLocationNumber(caseManagementLocation);
    }

    public void allocateRegionId(PCSCase pcsCase) {
        allocateCaseManagementLocation(pcsCase);
        if (pcsCase.getCaseManagementLocationNumber() != null) {
            log.info("Calling locationReferenceService.getCourtVenues(...) with {}",
                     pcsCase.getCaseManagementLocationNumber());
            List<CourtVenue> courtVenues = locationReferenceService
                .getCourtVenues(List.of(pcsCase.getCaseManagementLocationNumber()));
            log.info("Court venues are : {}", courtVenues);
            if (!CollectionUtils.isEmpty(courtVenues)) {
                pcsCase.setRegionId(Integer.valueOf(courtVenues.getFirst().regionId()));
            }
        }
    }

    public void patchCaseLinks(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        log.info("Patching linked cases for {}", caseReference);
        if (pcsCase.getCaseLinks() != null) {
            caseLinkService.mergeCaseLinks(pcsCase.getCaseLinks(), pcsCaseEntity);
        }
    }
}
