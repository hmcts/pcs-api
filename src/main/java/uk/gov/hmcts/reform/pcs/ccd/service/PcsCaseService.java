package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Objects;

@Service
@AllArgsConstructor
public class PcsCaseService {

    private final PcsCaseRepository pcsCaseRepository;
    private final PcsCaseMergeService pcsCaseMergeService;
    private final ModelMapper modelMapper;
    private final TenancyLicenceService tenancyLicenceService;
    private final PartyDocumentsService partyDocumentsService;

    public void createCase(long caseReference, AddressUK propertyAddress, LegislativeCountry legislativeCountry) {

        Objects.requireNonNull(propertyAddress, "Property address must be provided to create a case");
        Objects.requireNonNull(legislativeCountry, "Legislative country must be provided to create a case");

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(modelMapper.map(propertyAddress, AddressEntity.class));
        pcsCaseEntity.setLegislativeCountry(legislativeCountry);

        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void createCase(long caseReference, PCSCase pcsCase) {
        AddressUK applicantAddress = pcsCase.getPropertyAddress();

        AddressEntity addressEntity = applicantAddress != null
            ? modelMapper.map(applicantAddress, AddressEntity.class) : null;

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(addressEntity);
        pcsCaseEntity.setPreActionProtocolCompleted(
                pcsCase.getPreActionProtocolCompleted() != null
                        ? pcsCase.getPreActionProtocolCompleted().toBoolean()
                        : null);
        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.buildTenancyLicence(pcsCase));
        pcsCaseEntity.setPartyDocuments(partyDocumentsService.buildPartyDocuments(pcsCase));
        // Set claimant type if available
        if (pcsCase.getClaimantType() != null && pcsCase.getClaimantType().getValueCode() != null) {
            ClaimantType claimantType = ClaimantType.valueOf(pcsCase.getClaimantType().getValueCode());
            pcsCaseEntity.setClaimantType(claimantType);
        }

        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void patchCase(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        mergeCaseData(pcsCaseEntity, pcsCase);

        save(pcsCaseEntity);
    }

    public void mergeCaseData(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        pcsCaseMergeService.mergeCaseData(pcsCaseEntity, pcsCase);
        pcsCaseEntity.setPartyDocuments(partyDocumentsService.buildPartyDocuments(pcsCase));
    }

    public PcsCaseEntity loadCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    public void save(PcsCaseEntity pcsCaseEntity) {
        pcsCaseRepository.save(pcsCaseEntity);
    }

}
