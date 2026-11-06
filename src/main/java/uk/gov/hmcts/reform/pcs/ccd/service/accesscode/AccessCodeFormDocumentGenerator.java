package uk.gov.hmcts.reform.pcs.ccd.service.accesscode;

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
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.accesscode.AccessCodeFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * Renders a defendant access-code letter via Doc Assembly / Docmosis: English for every
 * defendant, and Welsh as well on a Wales case.
 */
@Service
@Slf4j
public class AccessCodeFormDocumentGenerator {

    static final String TEMPLATE_ID = "CV-PCS-LET-ENG-Defendant-Access-Code.docx";
    static final String TEMPLATE_ID_V2 = "CV-PCS-LET-ENG-Defendant-Access-Code-V2.docx";
    static final String WELSH_TEMPLATE_ID = "CV-PCS-LET-WEL-Defendant-Access-Code.docx";
    private static final String OUTPUT_FILENAME_PREFIX = "Defendant Access Code";
    private static final String PERSONS_UNKNOWN = "Persons unknown";
    // Supplied in Jira HDPI-9698. Welsh text: never edit or translate.
    private static final String PERSONS_UNKNOWN_WELSH = "Unigolion yn anhysbys";

    private final DocAssemblyService docAssemblyService;
    private final LocationReferenceService locationReferenceService;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final RecipientAddressResolver recipientAddressResolver;
    private final Clock ukClock;
    private final String respondOnlineUrl;
    private final FeatureToggleService featureToggleService;

    public AccessCodeFormDocumentGenerator(
        DocAssemblyService docAssemblyService,
        LocationReferenceService locationReferenceService,
        AddressMapper addressMapper,
        AddressFormatter addressFormatter,
        CaseReferenceFormatter caseReferenceFormatter,
        RecipientAddressResolver recipientAddressResolver,
        @Qualifier("ukClock") Clock ukClock,
        @Value("${access-code-form.respond-online-url}") String respondOnlineUrl,
        FeatureToggleService featureToggleService
    ) {
        this.docAssemblyService = docAssemblyService;
        this.locationReferenceService = locationReferenceService;
        this.addressMapper = addressMapper;
        this.addressFormatter = addressFormatter;
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.recipientAddressResolver = recipientAddressResolver;
        this.ukClock = ukClock;
        this.respondOnlineUrl = respondOnlineUrl;
        this.featureToggleService = featureToggleService;
    }

    /**
     * Resolves what every letter for a defendant shares: the respond-by-post court (a location reference lookup) and
     * the issue date. Call it once per defendant and pass the result to each {@link #generate} call, so a Wales
     * defendant's English and Welsh letters show the same court and date and the court is looked up once.
     */
    public LetterContext resolveLetterContext(PcsCaseEntity pcsCaseEntity) {
        return new LetterContext(resolveServingCourt(pcsCaseEntity), LocalDate.now(ukClock));
    }

    public String generate(PcsCaseEntity pcsCaseEntity,
                           ClaimEntity mainClaim,
                           PartyEntity defendant,
                           String plaintextAccessCode,
                           AccessCodeLetterLanguage language,
                           LetterContext context) {

        boolean welsh = language == AccessCodeLetterLanguage.WELSH;
        // The Welsh template has no V2 and no courtName tag, so release 1.4 changes only the English letter.
        boolean englishV2 = !welsh && featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4);
        String formattedPropertyAddress = formatAddress(pcsCaseEntity.getPropertyAddress());
        CourtVenue servingCourt = context.servingCourt();

        AccessCodeFormPayload payload = AccessCodeFormPayload.builder()
            .caseReference(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCaseEntity.getCaseReference()))
            .claimantName(resolveClaimantName(mainClaim))
            .defendantName(resolveDefendantName(defendant, welsh ? PERSONS_UNKNOWN_WELSH : PERSONS_UNKNOWN))
            .defendantAddress(resolveDefendantAddress(defendant, pcsCaseEntity))
            .propertyAddress(formattedPropertyAddress)
            .respondByPostCourtAddress(formatCourtAddress(servingCourt))
            .accessCode(plaintextAccessCode)
            .issuedOn(context.issuedOn())
            .url(respondOnlineUrl)
            .courtName(englishV2 ? servingCourt.courtName() : null)
            .build();

        String outputFilename = welsh
            ? AccessCodeDocuments.welshFileName(defendant.getId())
            : OUTPUT_FILENAME_PREFIX + " " + defendant.getId();

        return docAssemblyService.generateDocument(payload, templateId(welsh, englishV2), OutputType.PDF,
                                                   outputFilename);
    }

    private static String templateId(boolean welsh, boolean englishV2) {
        if (welsh) {
            return WELSH_TEMPLATE_ID;
        }
        return englishV2 ? TEMPLATE_ID_V2 : TEMPLATE_ID;
    }

    private String resolveClaimantName(ClaimEntity mainClaim) {
        return mainClaim.getClaimParties().stream()
            .filter(claimParty -> PartyRole.CLAIMANT == claimParty.getRole())
            .map(ClaimPartyEntity::getParty)
            .filter(party -> !party.isRemoved())
            .map(this::formatPartyName)
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse(null);
    }

    private String resolveDefendantName(PartyEntity defendant, String unknownName) {
        if (defendant.getNameKnown() == VerticalYesNo.YES) {
            String name = formatPartyName(defendant);
            if (StringUtils.isNotBlank(name)) {
                return name;
            }
        }
        return unknownName;
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

    private String resolveDefendantAddress(PartyEntity defendant, PcsCaseEntity pcsCaseEntity) {
        return formatAddress(recipientAddressResolver.resolvePostalAddress(
            defendant, PartyRole.DEFENDANT, pcsCaseEntity.getPropertyAddress()));
    }

    private CourtVenue resolveServingCourt(PcsCaseEntity pcsCaseEntity) {
        Integer epimsId = pcsCaseEntity.getCaseManagementLocation();
        if (epimsId == null) {
            throw new IllegalStateException(
                "No case management location set for case " + pcsCaseEntity.getCaseReference()
                    + "; cannot resolve respond-by-post court");
        }

        List<CourtVenue> venues = locationReferenceService.getCourtVenues(List.of(epimsId));
        if (venues == null || venues.isEmpty()) {
            throw new IllegalStateException(
                "No court venue found for epimsId " + epimsId + " on case " + pcsCaseEntity.getCaseReference()
                    + "; cannot resolve respond-by-post court");
        }
        return venues.getFirst();
    }

    private String formatCourtAddress(CourtVenue court) {
        if (court == null) {
            return null;
        }
        return Stream.of(court.courtName(), court.courtAddress(), court.postcode())
            .filter(StringUtils::isNotBlank)
            .flatMap(part -> Stream.of(part.split(",")))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .reduce((a, b) -> a + AddressFormatter.NEWLINE_DELIMITER + b)
            .orElse(null);
    }

    private String formatAddress(AddressEntity addressEntity) {
        if (addressEntity == null) {
            return null;
        }
        AddressUK addressUK = addressMapper.toAddressUK(addressEntity);
        return addressFormatter.formatFullAddressWithoutCountry(addressUK, AddressFormatter.NEWLINE_DELIMITER);
    }

    /**
     * The values shared by all of one defendant's access-code letters, from {@link #resolveLetterContext}.
     */
    public record LetterContext(CourtVenue servingCourt, LocalDate issuedOn) {
    }
}
