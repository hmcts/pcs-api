package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

@Service
@AllArgsConstructor
public class PcsCaseService {

    private final PCSCaseRepository pcsCaseRepository;
    private final ModelMapper modelMapper;

    public void createCase(long caseReference, PCSCase pcsCase) {
        AddressUK applicantAddress = pcsCase.getPropertyAddress();

        AddressEntity addressEntity = applicantAddress != null
            ? modelMapper.map(applicantAddress, AddressEntity.class) : null;

        PCSCaseEntity pcsCaseEntity = new PCSCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setApplicantForename(pcsCase.getApplicantForename());
        pcsCaseEntity.setApplicantSurname(pcsCase.getApplicantSurname());
        pcsCaseEntity.setPropertyAddress(addressEntity);

        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void patchCase(long caseReference, PCSCase pcsCase) {
        PCSCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        if (pcsCase.getApplicantForename() != null) {
            pcsCaseEntity.setApplicantForename(pcsCase.getApplicantForename());
        }

        if (pcsCase.getApplicantSurname() != null) {
            pcsCaseEntity.setApplicantSurname(pcsCase.getApplicantSurname());
        }

        if (pcsCase.getPropertyAddress() != null) {
            AddressEntity addressEntity = modelMapper.map(pcsCase.getPropertyAddress(), AddressEntity.class);
            pcsCaseEntity.setPropertyAddress(addressEntity);
        }

        pcsCaseRepository.save(pcsCaseEntity);
    }

}
