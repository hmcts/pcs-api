package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.document.model.genapp.GenAppFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

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
    private final SecurityContextService securityContextService;
    private final DocAssemblyService docAssemblyService;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final ModelMapper modelMapper;
    private final Clock ukClock;

    public GenAppDocumentGenerator(PcsCaseService pcsCaseService,
                                   PartyService partyService,
                                   SecurityContextService securityContextService,
                                   DocAssemblyService docAssemblyService,
                                   AddressMapper addressMapper,
                                   AddressFormatter addressFormatter,
                                   CaseNameFormatter caseNameFormatter,
                                   ModelMapper modelMapper,
                                   @Qualifier("ukClock") Clock ukClock) {
        this.pcsCaseService = pcsCaseService;
        this.partyService = partyService;
        this.securityContextService = securityContextService;
        this.docAssemblyService = docAssemblyService;
        this.addressMapper = addressMapper;
        this.addressFormatter = addressFormatter;
        this.caseNameFormatter = caseNameFormatter;
        this.modelMapper = modelMapper;
        this.ukClock = ukClock;
    }

    public String generateSubmissionDocument(long caseReference,
                                             CitizenGenAppRequest citizenGenAppRequest,
                                             GenAppEntity genAppEntity) {

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().getFirst();
        UUID applicantUserId = securityContextService.getCurrentUserId();
        String outputFilename = getDocumentFilename(mainClaim, genAppEntity, applicantUserId);

        GenAppFormPayload genAppFormPayload
            = createGenAppFormPayload(caseReference, pcsCaseEntity, mainClaim, citizenGenAppRequest, applicantUserId);

        return docAssemblyService
            .generateDocument(genAppFormPayload, TEMPLATE_ID, OutputType.PDF, outputFilename);
    }

    private GenAppFormPayload createGenAppFormPayload(long caseReference,
                                                      PcsCaseEntity pcsCaseEntity,
                                                      ClaimEntity mainClaim,
                                                      CitizenGenAppRequest citizenGenAppRequest,
                                                      UUID applicantUserId) {

        LocalDate currentUkDate = LocalDate.now(ukClock);

        String caseName = buildCaseName(mainClaim);

        PartyEntity applicantPartyEntity = partyService.getPartyEntityByIdamId(applicantUserId, caseReference);
        String applicantName = applicantPartyEntity.getFirstName() + " " + applicantPartyEntity.getLastName();
        String formattedPropertyAddress = getFormattedPropertyAddress(pcsCaseEntity);
        String formattedApplicantAddress = getFormattedApplicantAddress(applicantPartyEntity, formattedPropertyAddress);

        return GenAppFormPayload.builder()
            .caseReference(Long.toString(caseReference))
            .caseName(caseName)
            .submittedOn(currentUkDate)
            .propertyAddress(formattedPropertyAddress)
            .applicant(uk.gov.hmcts.reform.pcs.document.model.Party.builder()
                           .name(applicantName)
                           .correspondenceAddress(formattedApplicantAddress)
                           .emailAddress(applicantPartyEntity.getEmailAddress())
                           .telephoneNumber(applicantPartyEntity.getPhoneNumber())
                           .build())
            .applicationType(citizenGenAppRequest.getApplicationType())
            .within14Days(citizenGenAppRequest.getWithin14Days())
            .whatOrderWanted(citizenGenAppRequest.getWhatOrderWanted())
            .otherPartiesAgreed(citizenGenAppRequest.getOtherPartiesAgreed())
            .withoutNotice(citizenGenAppRequest.getWithoutNotice())
            .withoutNoticeReason(citizenGenAppRequest.getWithoutNoticeReason())
            .statementOfTruth(StatementOfTruth.builder()
                                  .fullName(citizenGenAppRequest.getSotFullName())
                                  .submittedOn(currentUkDate)
                                  .build()
            )
            .build();
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

    private String getDocumentFilename(ClaimEntity mainClaim, GenAppEntity genAppEntity, UUID applicantIdamId) {
        ClaimPartyEntity applicantClaimParty = getClaimParty(mainClaim, applicantIdamId);

        // Example label: General Application (GA2) - Defendant 1.pdf
        String applicantLabel = getPartyLabel(applicantClaimParty);
        String filename = "%s (GA%d)".formatted(OUTPUT_FILENAME_PREFIX, genAppEntity.getRank());
        if (applicantLabel != null) {
            filename += " - " + applicantLabel;
        }

        return filename;
    }

    private static ClaimPartyEntity getClaimParty(ClaimEntity claim, UUID partyIdamId) {
        return claim.getClaimParties().stream()
            .filter(claimPartyEntity -> partyIdamId.equals(claimPartyEntity.getParty().getIdamId()))
            .findFirst()
            .orElseThrow(() -> new PartyNotFoundException("Party not found"));
    }

    private static String getPartyLabel(ClaimPartyEntity applicantClaimParty) {
        if (applicantClaimParty.getRole() == PartyRole.CLAIMANT) {
            return "Claimant %d".formatted(applicantClaimParty.getRank());
        } else if (applicantClaimParty.getRole() == PartyRole.DEFENDANT) {
            return "Defendant %d".formatted(applicantClaimParty.getRank());
        } else {
            return null;
        }
    }

}
