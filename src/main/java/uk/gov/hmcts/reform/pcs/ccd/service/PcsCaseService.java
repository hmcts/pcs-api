package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class PcsCaseService {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;

    public PcsCaseEntity createCase(long caseReference, PCSCase pcsCase) {
        AddressUK applicantAddress = pcsCase.getPropertyAddress();

        AddressEntity addressEntity = applicantAddress != null
            ? modelMapper.map(applicantAddress, AddressEntity.class) : null;

        final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(addressEntity);
        pcsCaseEntity.setPaymentStatus(pcsCase.getPaymentStatus());
        pcsCaseEntity.setPreActionProtocolCompleted(
                pcsCase.getPreActionProtocolCompleted() != null
                        ? pcsCase.getPreActionProtocolCompleted().toBoolean()
                        : null);
        pcsCaseEntity.setDefendants(mapFromDefendantDetails(pcsCase.getDefendants()));

        // Process supporting documents
        List<ListValue<Document>> supportingDocuments = pcsCase.getSupportingDocuments();
        if (supportingDocuments != null && !supportingDocuments.isEmpty()) {
            for (ListValue<Document> documentWrapper : supportingDocuments) {
                if (documentWrapper != null && documentWrapper.getValue() != null) {
                    Document document = documentWrapper.getValue();

                    DocumentEntity documentEntity = new DocumentEntity();
                    documentEntity.setFileName(document.getFilename());
                    documentEntity.setFilePath(document.getBinaryUrl());
                    documentEntity.setUploadedOn(LocalDate.now());
                    documentEntity.setDocumentType("SUPPORTING");
                    documentEntity.setPcsCase(pcsCaseEntity);

                    pcsCaseEntity.addDocument(documentEntity);
                }
            }
        }

        // Process generated documents
        List<ListValue<Document>> generatedDocuments = pcsCase.getGeneratedDocuments();
        if (generatedDocuments != null && !generatedDocuments.isEmpty()) {
            for (ListValue<Document> documentWrapper : generatedDocuments) {
                if (documentWrapper != null && documentWrapper.getValue() != null) {
                    Document document = documentWrapper.getValue();

                    DocumentEntity documentEntity = new DocumentEntity();
                    documentEntity.setFileName(document.getFilename());
                    documentEntity.setFilePath(document.getBinaryUrl());
                    documentEntity.setUploadedOn(LocalDate.now());
                    documentEntity.setDocumentType("GENERATED");
                    documentEntity.setPcsCase(pcsCaseEntity);

                    pcsCaseEntity.addDocument(documentEntity);
                }
            }
        }
        log.info(String.valueOf(pcsCaseEntity.getDocuments().size()));
        pcsCaseEntity.setTenancyLicence(buildTenancyLicence(pcsCase));
        return pcsCaseRepository.save(pcsCaseEntity);
    }

    public void patchCase(long caseReference, PCSCase pcsCase) {
        final PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
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

    public void addDocumentToCase(long caseReference, String fileName, String filePath) {
        final PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        DocumentEntity document = new DocumentEntity();
        document.setFileName(fileName);
        document.setFilePath(filePath);
        document.setUploadedOn(LocalDate.now());
        document.setDocumentType("SUPPORTING");

        pcsCaseEntity.addDocument(document);
        pcsCaseRepository.save(pcsCaseEntity);
    }


    public void addGeneratedDocumentToCase(long caseReference, Document document) {
        final PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setFileName(document.getFilename());
        documentEntity.setFilePath(document.getBinaryUrl());
        documentEntity.setUploadedOn(LocalDate.now());
        documentEntity.setDocumentType("GENERATED");

        pcsCaseEntity.addDocument(documentEntity);
        pcsCaseRepository.save(pcsCaseEntity);
    }

    //Temporary method to create tenancy_licence JSON and related fields
    // Data in this JSON will likely be moved to a dedicated entity in the future
    private TenancyLicence buildTenancyLicence(PCSCase pcsCase) {
        return TenancyLicence.builder()
                .noticeServed(toBooleanOrNull(pcsCase.getNoticeServed()))
                .build();
    }

    private static Boolean toBooleanOrNull(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }
}
