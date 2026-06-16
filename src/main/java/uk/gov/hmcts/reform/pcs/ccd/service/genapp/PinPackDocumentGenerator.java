package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.PinPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * Renders the defendant pin pack (access-code letter) via Doc Assembly / Docmosis, one per defendant.
 */
@Service
@Slf4j
public class PinPackDocumentGenerator {

    static final String PIN_PACK_TEMPLATE_ID = "CV-PCS-LET-ENG-Defendant-Access-Code.docx";
    private static final String OUTPUT_FILENAME_PREFIX = "Defendant Access Code";
    private static final String PERSONS_UNKNOWN = "Persons unknown";

    private final DocAssemblyService docAssemblyService;
    private final LocationReferenceService locationReferenceService;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final Clock ukClock;
    private final String respondOnlineUrl;

    public PinPackDocumentGenerator(
        DocAssemblyService docAssemblyService,
        LocationReferenceService locationReferenceService,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        AddressMapper addressMapper,
        AddressFormatter addressFormatter,
        CaseReferenceFormatter caseReferenceFormatter,
        @Qualifier("ukClock") Clock ukClock,
        @Value("${pin-pack.respond-online-url}") String respondOnlineUrl
    ) {
        this.docAssemblyService = docAssemblyService;
        this.locationReferenceService = locationReferenceService;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.addressMapper = addressMapper;
        this.addressFormatter = addressFormatter;
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.ukClock = ukClock;
        this.respondOnlineUrl = respondOnlineUrl;
    }

    public String generatePinPack(PcsCaseEntity pcsCaseEntity,
                                  ClaimEntity mainClaim,
                                  PartyEntity defendant,
                                  String plaintextAccessCode) {

        String formattedPropertyAddress = formatAddress(pcsCaseEntity.getPropertyAddress());
        CourtVenue servingCourt = resolveServingCourt(pcsCaseEntity);

        PinPackFormPayload payload = PinPackFormPayload.builder()
            .caseReference(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCaseEntity.getCaseReference()))
            .claimantName(resolveClaimantName(mainClaim))
            .defendantName(resolveDefendantName(defendant))
            .defendantAddress(resolveDefendantAddress(defendant, formattedPropertyAddress))
            .propertyAddress(formattedPropertyAddress)
            .respondByPostCourtName(servingCourt != null ? servingCourt.courtName() : null)
            .respondByPostCourtAddress(formatCourtAddress(servingCourt))
            .accessCode(plaintextAccessCode)
            .issuedOn(LocalDate.now(ukClock))
            .url(respondOnlineUrl)
            .build();

        String outputFilename = OUTPUT_FILENAME_PREFIX + " " + defendant.getId();

        return docAssemblyService.generateDocument(payload, PIN_PACK_TEMPLATE_ID, OutputType.PDF, outputFilename);
    }

    private String resolveClaimantName(ClaimEntity mainClaim) {
        return mainClaim.getClaimParties().stream()
            .filter(claimParty -> PartyRole.CLAIMANT == claimParty.getRole())
            .map(ClaimPartyEntity::getParty)
            .map(this::formatPartyName)
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse(null);
    }

    private String resolveDefendantName(PartyEntity defendant) {
        if (defendant.getNameKnown() == VerticalYesNo.YES) {
            String name = formatPartyName(defendant);
            if (StringUtils.isNotBlank(name)) {
                return name;
            }
        }
        return PERSONS_UNKNOWN;
    }

    private String formatPartyName(PartyEntity party) {
        if (StringUtils.isNotBlank(party.getOrgName())) {
            return party.getOrgName();
        }
        return Stream.of(party.getFirstName(), party.getLastName())
            .filter(StringUtils::isNotBlank)
            .reduce((a, b) -> a + " " + b)
            .orElse(null);
    }

    private String resolveDefendantAddress(PartyEntity defendant, String formattedPropertyAddress) {
        if (defendant.getAddressKnown() == VerticalYesNo.YES
            && defendant.getAddressSameAsProperty() != VerticalYesNo.YES
            && defendant.getAddress() != null) {
            return formatAddress(defendant.getAddress());
        }
        return formattedPropertyAddress;
    }

    private CourtVenue resolveServingCourt(PcsCaseEntity pcsCaseEntity) {
        Integer epimsId = pcsCaseEntity.getCaseManagementLocation();
        if (epimsId == null) {
            log.warn("No case management location set for case {}; cannot resolve respond-by-post court",
                     pcsCaseEntity.getCaseReference());
            return null;
        }

        String authToken = systemUpdateUserTokenProvider.getAuthToken();
        List<CourtVenue> venues = locationReferenceService.getCountyCourts(authToken, List.of(epimsId));
        if (venues == null || venues.isEmpty()) {
            log.warn("No court venue found for epimsId {} on case {}", epimsId, pcsCaseEntity.getCaseReference());
            return null;
        }
        return venues.getFirst();
    }

    private String formatCourtAddress(CourtVenue court) {
        if (court == null) {
            return null;
        }
        return Stream.of(court.courtAddress(), court.postcode())
            .filter(StringUtils::isNotBlank)
            .reduce((a, b) -> a + AddressFormatter.NEWLINE_DELIMITER + b)
            .orElse(null);
    }

    private String formatAddress(AddressEntity addressEntity) {
        if (addressEntity == null) {
            return null;
        }
        AddressUK addressUK = addressMapper.toAddressUK(addressEntity);
        return addressFormatter.formatFullAddress(addressUK, AddressFormatter.NEWLINE_DELIMITER);
    }
}
