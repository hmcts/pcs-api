package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.Defendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PcsCaseService {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private TenancyLicenceService tenancyLicenceService;

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
        pcsCaseEntity.setDefendants(mapFromDefendantDetails(pcsCase));

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.buildTenancyLicence(pcsCase));

        pcsCaseRepository.save(pcsCaseEntity);
    }

    public PcsCaseEntity patchCase(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        if (pcsCase.getPropertyAddress() != null) {
            AddressEntity addressEntity = modelMapper.map(pcsCase.getPropertyAddress(), AddressEntity.class);
            pcsCaseEntity.setPropertyAddress(addressEntity);
        }

        if (pcsCase.getUserPcqId() != null) {
            UUID pcqId = UUID.fromString(pcsCase.getUserPcqId());
            setPcqIdForCurrentUser(pcqId, pcsCaseEntity);
        }

        if (pcsCase.getPaymentStatus() != null) {
            pcsCaseEntity.setPaymentStatus(pcsCase.getPaymentStatus());
        }

        if (pcsCase.getCaseManagementLocation() != null) {
            pcsCaseEntity.setCaseManagementLocation(pcsCase.getCaseManagementLocation());
        }

        if (pcsCase.getPreActionProtocolCompleted() != null) {
            pcsCaseEntity.setPreActionProtocolCompleted(pcsCase.getPreActionProtocolCompleted().toBoolean());
        }

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.buildTenancyLicence(pcsCase));

        pcsCaseRepository.save(pcsCaseEntity);

        return pcsCaseEntity;
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

    public List<Defendant> mapFromDefendantDetails(PCSCase pcsCase) {
        if (pcsCase == null) {
            return Collections.emptyList();
        }
        List<Defendant> result = new ArrayList<>();
        DefendantDetails[] defendantArray = {
            pcsCase.getDefendant1(), pcsCase.getDefendant2(), pcsCase.getDefendant3(),
            pcsCase.getDefendant4(), pcsCase.getDefendant5(), pcsCase.getDefendant6(),
            pcsCase.getDefendant7(), pcsCase.getDefendant8(), pcsCase.getDefendant9(),
            pcsCase.getDefendant10(), pcsCase.getDefendant11(), pcsCase.getDefendant12(),
            pcsCase.getDefendant13(), pcsCase.getDefendant14(), pcsCase.getDefendant15(),
            pcsCase.getDefendant16(), pcsCase.getDefendant17(), pcsCase.getDefendant18(),
            pcsCase.getDefendant19(), pcsCase.getDefendant20(), pcsCase.getDefendant21(),
            pcsCase.getDefendant22(), pcsCase.getDefendant23(), pcsCase.getDefendant24(),
            pcsCase.getDefendant25()
        };
        for (int i = 0; i < defendantArray.length; i++) {
            DefendantDetails details = defendantArray[i];
            if (details != null) {
                Defendant defendant = modelMapper.map(details, Defendant.class);
                defendant.setId(String.valueOf(i + 1));
                if (details.getAddressSameAsPossession() == null) {
                    defendant.setAddressSameAsPossession(false);
                }
                result.add(defendant);
            }
        }
        return result;
    }

    public List<Defendant> mapFromDefendants(Defendants defendants) {
        if (defendants == null) {
            return Collections.emptyList();
        }
        List<Defendant> result = new ArrayList<>();
        
        // Map all defendant fields (1-25)
        DefendantDetails[] defendantArray = {
            defendants.getDefendant1(), defendants.getDefendant2(), defendants.getDefendant3(),
            defendants.getDefendant4(), defendants.getDefendant5(), defendants.getDefendant6(),
            defendants.getDefendant7(), defendants.getDefendant8(), defendants.getDefendant9(),
            defendants.getDefendant10(), defendants.getDefendant11(), defendants.getDefendant12(),
            defendants.getDefendant13(), defendants.getDefendant14(), defendants.getDefendant15(),
            defendants.getDefendant16(), defendants.getDefendant17(), defendants.getDefendant18(),
            defendants.getDefendant19(), defendants.getDefendant20(), defendants.getDefendant21(),
            defendants.getDefendant22(), defendants.getDefendant23(), defendants.getDefendant24(),
            defendants.getDefendant25()
        };
        
        for (int i = 0; i < defendantArray.length; i++) {
            DefendantDetails details = defendantArray[i];
            if (details != null) {
                Defendant defendant = modelMapper.map(details, Defendant.class);
                defendant.setId(String.valueOf(i + 1)); // Use index as ID
                if (details.getAddressSameAsPossession() == null) {
                    defendant.setAddressSameAsPossession(false);
                }
                result.add(defendant);
            }
        }
        return result;
    }

    public Defendants mapToDefendants(List<Defendant> defendants) {
        if (defendants == null || defendants.isEmpty()) {
            return Defendants.builder().build();
        }
        
        Defendants.DefendantsBuilder builder = Defendants.builder();
        
        // Map defendants to the appropriate fields based on their ID
        for (Defendant defendant : defendants) {
            if (defendant.getId() != null) {
                try {
                    int index = Integer.parseInt(defendant.getId());
                    if (index >= 1 && index <= 25) {
                        DefendantDetails details = modelMapper.map(defendant, DefendantDetails.class);
                        switch (index) {
                            case 1 -> builder.defendant1(details);
                            case 2 -> builder.defendant2(details);
                            case 3 -> builder.defendant3(details);
                            case 4 -> builder.defendant4(details);
                            case 5 -> builder.defendant5(details);
                            case 6 -> builder.defendant6(details);
                            case 7 -> builder.defendant7(details);
                            case 8 -> builder.defendant8(details);
                            case 9 -> builder.defendant9(details);
                            case 10 -> builder.defendant10(details);
                            case 11 -> builder.defendant11(details);
                            case 12 -> builder.defendant12(details);
                            case 13 -> builder.defendant13(details);
                            case 14 -> builder.defendant14(details);
                            case 15 -> builder.defendant15(details);
                            case 16 -> builder.defendant16(details);
                            case 17 -> builder.defendant17(details);
                            case 18 -> builder.defendant18(details);
                            case 19 -> builder.defendant19(details);
                            case 20 -> builder.defendant20(details);
                            case 21 -> builder.defendant21(details);
                            case 22 -> builder.defendant22(details);
                            case 23 -> builder.defendant23(details);
                            case 24 -> builder.defendant24(details);
                            case 25 -> builder.defendant25(details);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid IDs
                }
            }
        }
        
        return builder.build();
    }

    public Defendants mapToDefendants(PCSCase pcsCase) {
        if (pcsCase == null) {
            return Defendants.builder().build();
        }
        
        Defendants.DefendantsBuilder builder = Defendants.builder();
        
        // Map all defendant fields (1-25)
        builder.defendant1(pcsCase.getDefendant1())
               .defendant2(pcsCase.getDefendant2())
               .defendant3(pcsCase.getDefendant3())
               .defendant4(pcsCase.getDefendant4())
               .defendant5(pcsCase.getDefendant5())
               .defendant6(pcsCase.getDefendant6())
               .defendant7(pcsCase.getDefendant7())
               .defendant8(pcsCase.getDefendant8())
               .defendant9(pcsCase.getDefendant9())
               .defendant10(pcsCase.getDefendant10())
               .defendant11(pcsCase.getDefendant11())
               .defendant12(pcsCase.getDefendant12())
               .defendant13(pcsCase.getDefendant13())
               .defendant14(pcsCase.getDefendant14())
               .defendant15(pcsCase.getDefendant15())
               .defendant16(pcsCase.getDefendant16())
               .defendant17(pcsCase.getDefendant17())
               .defendant18(pcsCase.getDefendant18())
               .defendant19(pcsCase.getDefendant19())
               .defendant20(pcsCase.getDefendant20())
               .defendant21(pcsCase.getDefendant21())
               .defendant22(pcsCase.getDefendant22())
               .defendant23(pcsCase.getDefendant23())
               .defendant24(pcsCase.getDefendant24())
               .defendant25(pcsCase.getDefendant25())
               .addAnotherDefendant1(pcsCase.getAddAnotherDefendant1())
               .addAnotherDefendant2(pcsCase.getAddAnotherDefendant2())
               .addAnotherDefendant3(pcsCase.getAddAnotherDefendant3())
               .addAnotherDefendant4(pcsCase.getAddAnotherDefendant4())
               .addAnotherDefendant5(pcsCase.getAddAnotherDefendant5())
               .addAnotherDefendant6(pcsCase.getAddAnotherDefendant6())
               .addAnotherDefendant7(pcsCase.getAddAnotherDefendant7())
               .addAnotherDefendant8(pcsCase.getAddAnotherDefendant8())
               .addAnotherDefendant9(pcsCase.getAddAnotherDefendant9())
               .addAnotherDefendant10(pcsCase.getAddAnotherDefendant10())
               .addAnotherDefendant11(pcsCase.getAddAnotherDefendant11())
               .addAnotherDefendant12(pcsCase.getAddAnotherDefendant12())
               .addAnotherDefendant13(pcsCase.getAddAnotherDefendant13())
               .addAnotherDefendant14(pcsCase.getAddAnotherDefendant14())
               .addAnotherDefendant15(pcsCase.getAddAnotherDefendant15())
               .addAnotherDefendant16(pcsCase.getAddAnotherDefendant16())
               .addAnotherDefendant17(pcsCase.getAddAnotherDefendant17())
               .addAnotherDefendant18(pcsCase.getAddAnotherDefendant18())
               .addAnotherDefendant19(pcsCase.getAddAnotherDefendant19())
               .addAnotherDefendant20(pcsCase.getAddAnotherDefendant20())
               .addAnotherDefendant21(pcsCase.getAddAnotherDefendant21())
               .addAnotherDefendant22(pcsCase.getAddAnotherDefendant22())
               .addAnotherDefendant23(pcsCase.getAddAnotherDefendant23())
               .addAnotherDefendant24(pcsCase.getAddAnotherDefendant24())
               .addAnotherDefendant25(pcsCase.getAddAnotherDefendant25());
        
        return builder.build();
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

    private void setPcqIdForCurrentUser(UUID pcqId, PcsCaseEntity pcsCaseEntity) {
        UserInfo userDetails = securityContextService.getCurrentUserDetails();
        UUID userId = UUID.fromString(userDetails.getUid());
        pcsCaseEntity.getParties().stream()
            .filter(party -> userId.equals(party.getIdamId()))
            .findFirst()
            .orElseGet(() -> {
                PartyEntity party = createPartyForUser(userId, userDetails);
                pcsCaseEntity.addParty(party);
                return party;
            })
            .setPcqId(pcqId);
    }

    private static PartyEntity createPartyForUser(UUID userId, UserInfo userDetails) {
        PartyEntity party = new PartyEntity();
        party.setIdamId(userId);
        party.setForename(userDetails.getGivenName());
        party.setSurname(userDetails.getFamilyName());
        party.setActive(true);
        return party;
    }

}
