package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.Document;
import uk.gov.hmcts.reform.pcs.document.model.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.document.model.genapp.GenAppFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GenAppDocumentGenerator {

    private static final String TEMPLATE_ID = "CV-PCS-GAP-ENG-Application-Summary.docx";
    private static final String OUTPUT_FILENAME_PREFIX = "General Application";

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final DocAssemblyService docAssemblyService;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final DocumentNameService documentNameService;
    private final DocumentImportService documentImportService;
    private final ModelMapper modelMapper;
    private final Clock ukClock;

    public GenAppDocumentGenerator(PcsCaseService pcsCaseService,
                                   PartyService partyService,
                                   DocAssemblyService docAssemblyService,
                                   AddressMapper addressMapper,
                                   AddressFormatter addressFormatter,
                                   CaseReferenceFormatter caseReferenceFormatter,
                                   CaseNameFormatter caseNameFormatter,
                                   DocumentNameService documentNameService,
                                   DocumentImportService documentImportService,
                                   ModelMapper modelMapper,
                                   @Qualifier("ukClock") Clock ukClock) {
        this.pcsCaseService = pcsCaseService;
        this.partyService = partyService;
        this.docAssemblyService = docAssemblyService;
        this.addressMapper = addressMapper;
        this.addressFormatter = addressFormatter;
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.caseNameFormatter = caseNameFormatter;
        this.documentNameService = documentNameService;
        this.documentImportService = documentImportService;
        this.modelMapper = modelMapper;
        this.ukClock = ukClock;
    }

    public void createSubmissionDocument(long caseReference,
                                         GenAppEntity genAppEntity) {

        PartyEntity applicantParty = genAppEntity.getParty();

        String documentUrl = generateSubmissionDocument(
            caseReference,
            genAppEntity,
            applicantParty
        );

        DocumentEntity importedDocumentEntity = documentImportService.addDocumentToCase(
            caseReference,
            documentUrl,
            CaseFileCategory.APPLICATIONS
        );

        importedDocumentEntity.setGeneralApplication(genAppEntity);
        genAppEntity.setSubmissionDocument(importedDocumentEntity);
    }

    private String generateSubmissionDocument(long caseReference,
                                              GenAppEntity genAppEntity,
                                              PartyEntity applicantParty) {

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        UUID applicantPartyId = applicantParty.getId();
        String outputFilename = documentNameService
            .appendGenAppPostfix(OUTPUT_FILENAME_PREFIX, genAppEntity, mainClaim, applicantPartyId);

        GenAppFormPayload genAppFormPayload = createGenAppFormPayload(caseReference,
                                                                      pcsCaseEntity,
                                                                      mainClaim,
                                                                      genAppEntity,
                                                                      applicantPartyId);

        return docAssemblyService
            .generateDocument(genAppFormPayload, TEMPLATE_ID, OutputType.PDF, outputFilename);
    }

    private GenAppFormPayload createGenAppFormPayload(long caseReference,
                                                      PcsCaseEntity pcsCaseEntity,
                                                      ClaimEntity mainClaim,
                                                      GenAppEntity genAppEntity,
                                                      UUID applicantPartyId) {

        LocalDate currentUkDate = LocalDate.now(ukClock);

        String caseName = buildCaseName(mainClaim);

        PartyEntity applicantPartyEntity = partyService.getPartyEntityByEntityId(applicantPartyId, caseReference);
        String applicantName = applicantPartyEntity.getFirstName() + " " + applicantPartyEntity.getLastName();
        String formattedPropertyAddress = getFormattedPropertyAddress(pcsCaseEntity);
        String formattedApplicantAddress = getFormattedApplicantAddress(applicantPartyEntity, formattedPropertyAddress);

        StatementOfTruthEntity statementOfTruth = genAppEntity.getStatementOfTruth();

        return GenAppFormPayload.builder()
            .caseReference(caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference))
            .caseName(caseName)
            .submittedOn(currentUkDate)
            .issuedOn(currentUkDate)
            .propertyAddress(formattedPropertyAddress)
            .applicant(uk.gov.hmcts.reform.pcs.document.model.Party.builder()
                           .name(applicantName)
                           .correspondenceAddress(formattedApplicantAddress)
                           .emailAddress(applicantPartyEntity.getEmailAddress())
                           .telephoneNumber(applicantPartyEntity.getPhoneNumber())
                           .build())
            .applicationType(genAppEntity.getType())
            .within14Days(genAppEntity.getWithin14Days())
            .whatOrderWanted(genAppEntity.getWhatOrderWanted())
            .otherPartiesAgreed(genAppEntity.getOtherPartiesAgreed())
            .withoutNotice(genAppEntity.getWithoutNotice())
            .withoutNoticeReason(genAppEntity.getWithoutNoticeReason())
            .documentUploadWanted(genAppEntity.getDocumentsUploaded())
            .uploadedDocuments(getDocumentList(genAppEntity))
            .statementOfTruth(StatementOfTruth.builder()
                                  .fullName(statementOfTruth != null ? statementOfTruth.getFullName() : null)
                                  .submittedOn(currentUkDate)
                                  .build()
            )
            .build();
    }

    private List<Document> getDocumentList(GenAppEntity genAppEntity) {
        List<DocumentEntity> uploadedDocuments = genAppEntity.getDocuments();

        return uploadedDocuments.stream()
            .map(documentEntity -> Document.builder().filename(documentEntity.getFileName()).build())
            .toList();
    }

    private String buildCaseName(ClaimEntity mainClaim) {
        Map<PartyRole, List<Party>> partyMap = getPartyMap(mainClaim);

        List<Party> claimants = partyMap.get(PartyRole.CLAIMANT);
        List<Party> defendants = partyMap.get(PartyRole.DEFENDANT);

        return caseNameFormatter.formatCaseName(claimants, defendants);
    }

    private String getFormattedPropertyAddress(PcsCaseEntity pcsCaseEntity) {
        AddressEntity propertyAddress = pcsCaseEntity.getPropertyAddress();
        return formatAddress(propertyAddress);
    }

    private String getFormattedApplicantAddress(PartyEntity partyEntity, String formattedPropertyAddress) {
        String formattedPartyAddress = null;
        if (partyEntity.getAddressKnown() == VerticalYesNo.YES) {
            if (partyEntity.getAddressSameAsProperty() == VerticalYesNo.YES) {
                formattedPartyAddress = formattedPropertyAddress;
            } else {
                formattedPartyAddress = formatAddress(partyEntity.getAddress());
            }
        }
        return formattedPartyAddress;
    }

    private String formatAddress(AddressEntity propertyAddress) {
        AddressUK addressUK = addressMapper.toAddressUK(propertyAddress);
        return addressFormatter.formatFullAddress(addressUK, AddressFormatter.NEWLINE_DELIMITER);
    }

    private Map<PartyRole, List<Party>> getPartyMap(ClaimEntity claim) {
        return claim.getClaimParties().stream()
            .collect(Collectors.groupingBy(
                ClaimPartyEntity::getRole,
                Collectors.mapping(this::toParty, Collectors.toList())
            ));
    }

    private Party toParty(ClaimPartyEntity claimPartyEntity) {
        return modelMapper.map(claimPartyEntity.getParty(), Party.class);
    }

}
