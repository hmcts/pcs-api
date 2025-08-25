package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

        List<ListValue<Document>> supportingDocuments = pcsCase.getSupportingDocuments();
        if (supportingDocuments != null && !supportingDocuments.isEmpty()) {
            for (ListValue<Document> documentWrapper : supportingDocuments) {
                if (documentWrapper != null && documentWrapper.getValue() != null) {
                    Document document = documentWrapper.getValue();

                    DocumentEntity documentEntity = new DocumentEntity();
                    documentEntity.setFileName(document.getFilename());
                    documentEntity.setFilePath(document.getBinaryUrl());
                    documentEntity.setUploadedOn(LocalDate.now());
                    documentEntity.setPcsCase(pcsCaseEntity);

                    pcsCaseEntity.addDocument(documentEntity);
                }
            }
        }

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

        pcsCaseEntity.addDocument(document);
        pcsCaseRepository.save(pcsCaseEntity);
    }

}
