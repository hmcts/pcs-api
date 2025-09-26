package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class PcsCaseService {

    private final PcsCaseRepository pcsCaseRepository;
    private final PcsCaseMergeService pcsCaseMergeService;
    private final ModelMapper modelMapper;
    private final TenancyLicenceService tenancyLicenceService;

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
        pcsCaseEntity.setPaymentStatus(pcsCase.getPaymentStatus());
        pcsCaseEntity.setPreActionProtocolCompleted(
                pcsCase.getPreActionProtocolCompleted() != null
                        ? pcsCase.getPreActionProtocolCompleted().toBoolean()
                        : null);
        pcsCaseEntity.setDefendants(mapFromDefendantDetails(pcsCase.getDefendants()));
        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.buildTenancyLicence(pcsCase));

        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void patchCase(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        mergeCaseData(pcsCaseEntity, pcsCase);

        save(pcsCaseEntity);
    }

    public void mergeCaseData(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        pcsCaseMergeService.mergeCaseData(pcsCaseEntity, pcsCase);
    }

    public PcsCaseEntity loadCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    public void save(PcsCaseEntity pcsCaseEntity) {
        pcsCaseRepository.save(pcsCaseEntity);
    }

    public List<Defendant> mapFromDefendantDetails(List<ListValue<DefendantDetails>> defendants) {
        if (defendants == null) {
            return Collections.emptyList();
        }
        List<Defendant> result = new ArrayList<>();
        for (ListValue<DefendantDetails> item : defendants) {
            DefendantDetails details = item.getValue();
            if (details != null) {
                Defendant defendant = modelMapper.map(details, Defendant.class);
                defendant.setId(item.getId());
                if (details.getAddressSameAsPossession() == null) {
                    defendant.setAddressSameAsPossession(false);
                }
                result.add(defendant);
            }
        }
        return result;
    }

    public List<ListValue<DefendantDetails>> mapToDefendantDetails(List<Defendant> defendants) {
        if (defendants == null) {
            return Collections.emptyList();
        }
        List<ListValue<DefendantDetails>> result = new ArrayList<>();
        for (Defendant defendant : defendants) {
            if (defendant != null) {
                DefendantDetails details = modelMapper.map(defendant, DefendantDetails.class);
                result.add(new ListValue<>(defendant.getId(), details));
            }
        }
        return result;
    }

    public void clearHiddenDefendantDetailsFields(List<ListValue<DefendantDetails>> defendantsList) {
        if (defendantsList == null) {
            return;
        }

        for (ListValue<DefendantDetails> listValue : defendantsList) {
            DefendantDetails defendant = listValue.getValue();
            if (defendant != null) {
                if (VerticalYesNo.NO == defendant.getNameKnown()) {
                    defendant.setFirstName(null);
                    defendant.setLastName(null);
                }
                if (VerticalYesNo.NO == defendant.getAddressKnown()) {
                    defendant.setCorrespondenceAddress(null);
                    defendant.setAddressSameAsPossession(null);
                }
                if (VerticalYesNo.NO == defendant.getEmailKnown()) {
                    defendant.setEmail(null);
                }
            }
        }
    }

}
