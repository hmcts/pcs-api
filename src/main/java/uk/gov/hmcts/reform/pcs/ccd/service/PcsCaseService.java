package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

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

    public void createMainClaimOnCase(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        ClaimEntity claimEntity = claimService.createMainClaimEntity(pcsCase);
        List<DocumentEntity> documentEntities = documentService.createAllDocuments(pcsCase);
        pcsCaseEntity.addDocuments(documentEntities);
        claimEntity.addClaimDocuments(documentEntities);
        pcsCaseEntity.addClaim(claimEntity);

        partyService.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.createTenancyLicenceEntity(pcsCase));
    }

    public void patchCaseFlags(long caseReference, PCSCase pcsCase) {
        if (pcsCase == null) {
            throw new IllegalArgumentException("PCSCase cannot be null");
        }
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        if (pcsCase.getCaseFlags() != null) {
            caseFlagService.mergeCaseFlags(pcsCase.getCaseFlags(), pcsCaseEntity);
        }

        if (pcsCase.getParties() != null) {
            caseFlagService.mergePartyFlags(pcsCase.getParties(), pcsCaseEntity);
        }
    }

    public PcsCaseEntity loadCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    public void patchCaseLinks(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        log.info("Patching linked cases for {}", caseReference);
        if (pcsCase.getCaseLinks() != null) {
            caseLinkService.mergeCaseLinks(pcsCase.getCaseLinks(), pcsCaseEntity);
        }
    }
}
