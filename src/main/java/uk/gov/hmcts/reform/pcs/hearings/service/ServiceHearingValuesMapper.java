package uk.gov.hmcts.reform.pcs.hearings.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.hearings.config.ServiceHearingValuesConfiguration;
import uk.gov.hmcts.reform.pcs.hearings.model.CaseFlags;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingWindow;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.OrganisationDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.PartyDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.PartyFlags;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Builds the serviceHearingValues payload that ExUI requests when a user starts a hearing request.
 * Values that are the same for every Possessions case come from {@link ServiceHearingValuesConfiguration};
 * values that depend on the case are then derived from PCS data.
 */
@Slf4j
@Service
public class ServiceHearingValuesMapper {

    static final int DUTY_SOLICITOR_ATTENDEES = 1;
    static final String CASE_FLAGS_PREFIX = "Case Flags: ";
    static final String ACTIVE_FLAG_STATUS = "Active";
    static final String URGENT_CASE_FLAG_CODE = "CF0007";
    static final String PARTY_TYPE_INDIVIDUAL = "IND";
    static final String PARTY_TYPE_ORGANISATION = "ORG";
    static final String UNDERLESSEE_OR_MORTGAGEE_LAST_NAME = "N/A";
    static final String OTHER_FLAG_CODE = "OT0001";
    static final int MAX_LISTING_COMMENTS_LENGTH = 2000;
    static final String ELLIPSIS = "...";
    private static final String FLAG_SEPARATOR = ", ";
    private static final String COMMENT_OPEN = " (Comments: ";
    private static final String COMMENT_CLOSE = ")";
    private static final String CASE_FILE_VIEW_PATH = "/cases/case-details/%d#Case%%20File%%20View";
    static final String CASE_FLAGS_PATH = "/cases/case-details/%d#Case%%20flags";
    static final String CASE_LEVEL_FLAGS_PARTY_NAME = "Case level flags";
    private static final String HEARING_PRIORITY_URGENT = "Urgent";
    private static final String PERSONS_UNKNOWN = "Persons unknown";

    private final ServiceHearingValuesConfiguration defaults;
    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final CaseNameFormatter caseNameFormatter;
    private final IndividualDetailsFlagMapper individualDetailsFlagMapper;
    private final String manageCaseUrl;

    public ServiceHearingValuesMapper(ServiceHearingValuesConfiguration defaults,
                                      PcsCaseService pcsCaseService,
                                      PcsCaseRepository pcsCaseRepository,
                                      CaseNameFormatter caseNameFormatter,
                                      IndividualDetailsFlagMapper individualDetailsFlagMapper,
                                      @Value("${manage_case.url}") String manageCaseUrl) {
        this.defaults = defaults;
        this.pcsCaseService = pcsCaseService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.caseNameFormatter = caseNameFormatter;
        this.individualDetailsFlagMapper = individualDetailsFlagMapper;
        this.manageCaseUrl = manageCaseUrl;
    }

    @Transactional(readOnly = true)
    public ServiceHearingValues getServiceHearingValues(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ServiceHearingValues values = defaults.getDefaultValues();

        String caseName = caseNameFormatter.formatCaseName(pcsCase);
        values.setCaseId(String.valueOf(pcsCase.getCaseReference()));
        values.setHmctsInternalCaseName(caseName);
        values.setPublicCaseName(caseName);
        values.setCaseDeepLink(caseDeepLink(pcsCase.getCaseReference()));
        values.setCaseSlaStartDate(getClaimIssuedDate(pcsCase));
        values.setHearingWindow(hearingWindow(pcsCase));
        if (isUrgentHearing(pcsCase)) {
            values.setHearingPriorityType(HEARING_PRIORITY_URGENT);
        }
        values.setNumberOfPhysicalAttendees(numberOfPhysicalAttendees(pcsCase));
        values.setListingComments(listingComments(pcsCase));
        String caseManagementLocation = Objects.toString(pcsCase.getCaseManagementLocation(), null);
        values.setCaseManagementLocationCode(caseManagementLocation);
        values.getHearingLocations().forEach(location -> location.setLocationId(caseManagementLocation));
        List<PartyDetails> parties = parties(pcsCase);
        if (!parties.isEmpty()) {
            values.setParties(parties);
        }
        values.setCaseFlags(caseFlags(pcsCase, parties));

        log.info("Built service hearing values for case {}", caseReference);
        return values;
    }

    @Transactional(readOnly = true)
    public List<ServiceLinkedCases> getServiceLinkedCases(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);

        Map<Long, PcsCaseEntity> linkedCasesByReference = findLinkedCases(pcsCase.getCaseLinks());

        return pcsCase.getCaseLinks().stream()
            .map(link -> ServiceLinkedCases.builder()
                .caseReference(String.valueOf(link.getLinkedCaseReference()))
                .caseName(Optional.ofNullable(linkedCasesByReference.get(link.getLinkedCaseReference()))
                    .map(caseNameFormatter::formatCaseName)
                    .orElse(null))
                .reasonsForLink(link.getReasons().stream().map(CaseLinkReasonEntity::getReasonCode).toList())
                .build())
            .toList();
    }

    private Map<Long, PcsCaseEntity> findLinkedCases(List<CaseLinkEntity> caseLinks) {
        List<Long> linkedCaseReferences = caseLinks.stream()
            .map(CaseLinkEntity::getLinkedCaseReference)
            .distinct()
            .toList();

        if (linkedCaseReferences.isEmpty()) {
            return Map.of();
        }

        return pcsCaseRepository.findAllByCaseReferenceIn(linkedCaseReferences).stream()
            .collect(Collectors.toMap(PcsCaseEntity::getCaseReference, Function.identity()));
    }

    private String caseDeepLink(long caseReference) {
        return manageCaseUrl + String.format(CASE_FILE_VIEW_PATH, caseReference);
    }

    private LocalDate getClaimIssuedDate(PcsCaseEntity pcsCase) {
        return Optional.ofNullable(pcsCase.getMainClaim())
            .map(ClaimEntity::getClaimIssuedDate)
            .map(issued -> issued.atZone(ZoneOffset.UTC).withZoneSameInstant(UK_ZONE_ID).toLocalDate())
            .orElse(null);
    }

    private HearingWindow hearingWindow(PcsCaseEntity pcsCase) {
        LocalDate claimIssuedDate = getClaimIssuedDate(pcsCase);
        if (claimIssuedDate == null) {
            return null;
        }
        HearingWindow hearingWindow = new HearingWindow();
        hearingWindow.setDateRangeStart(claimIssuedDate.plusDays(defaults.getNumberOfDaysStartOffset()));
        hearingWindow.setDateRangeEnd(claimIssuedDate.plusDays(defaults.getNumberOfDaysOffset()));
        return hearingWindow;
    }

    private boolean isUrgentHearing(PcsCaseEntity pcsCase) {
        return hasActiveUrgentFlag(pcsCase) || hasOpenSuspendApplication(pcsCase);
    }

    private boolean hasActiveUrgentFlag(PcsCaseEntity pcsCase) {
        return pcsCase.getCaseFlags().stream()
            .filter(flag -> ACTIVE_FLAG_STATUS.equals(flag.getDefaultStatus()))
            .map(BaseCaseFlag::getFlagRefData)
            .filter(Objects::nonNull)
            .anyMatch(flagRefData -> URGENT_CASE_FLAG_CODE.equals(flagRefData.getFlagCode()));
    }

    private boolean hasOpenSuspendApplication(PcsCaseEntity pcsCase) {
        return pcsCase.getGenApps().stream()
            .anyMatch(genApp -> genApp.getType() == GenAppType.SUSPEND
                && genApp.isOpen());
    }
    
    private List<PartyDetails> parties(PcsCaseEntity pcsCase) {
        return activeParties(pcsCase)
            .map(this::partyDetails)
            .toList();
    }

    private static Stream<PartyEntity> activeParties(PcsCaseEntity pcsCase) {
        return pcsCase.getParties().stream()
            .filter(Objects::nonNull)
            .filter(party -> !party.isRemoved());
    }

    // defendantFlags is the historical name on PartyEntity for a person's flags, including claimants
    private PartyDetails partyDetails(PartyEntity party) {
        String partyType = partyType(party);
        if (PARTY_TYPE_ORGANISATION.equals(partyType)) {
            return PartyDetails.builder()
                .partyID(party.getId().toString())
                .partyType(partyType)
                .partyRole(party.getEntityRoleCodeKey())
                .organisationDetails(OrganisationDetails.builder()
                    .name(party.getOrgName())
                    .organisationType(PARTY_TYPE_ORGANISATION)
                    .cftOrganisationID(party.getOrganisationId())
                    .build())
                .build();
        }

        IndividualDetails details = individualDetailsFlagMapper.map(party.getDefendantFlags());
        details.setPreferredHearingChannel(defaults.getPreferredHearingChannel());
        if (isUnderlesseeOrMortgagee(party)) {
            details.setFirstName(party.getOrgName());
            details.setLastName(UNDERLESSEE_OR_MORTGAGEE_LAST_NAME);
        } else if (party.getNameKnown() == VerticalYesNo.NO) {
            details.setFirstName(PERSONS_UNKNOWN);
            details.setLastName(PERSONS_UNKNOWN);
        } else {
            details.setFirstName(party.getFirstName());
            details.setLastName(party.getLastName());
        }
        details.setHearingChannelEmail(contactList(party.getEmailAddress()));
        details.setHearingChannelPhone(contactList(party.getPhoneNumber()));
        return PartyDetails.builder()
            .partyID(party.getId().toString())
            .partyType(partyType)
            .partyRole(party.getEntityRoleCodeKey())
            .individualDetails(details)
            .organisationDetails(legalRepOrganisationDetails(party))
            .build();
    }

    private static String partyType(PartyEntity party) {
        // party.type is not set on model and is null in the database
        return party.isClaimCreator() && party.getOrganisationId() != null
            ? PARTY_TYPE_ORGANISATION
            : PARTY_TYPE_INDIVIDUAL;
    }

    private static boolean isUnderlesseeOrMortgagee(PartyEntity party) {
        // the underlessee or mortgagee name is a single free-text field stored in orgName
        return party.getClaimParties().stream()
            .anyMatch(claimParty -> claimParty.getRole() == PartyRole.UNDERLESSEE_OR_MORTGAGEE);
    }

    private static OrganisationDetails legalRepOrganisationDetails(PartyEntity party) {
        if (party.isClaimCreator()) {
            return null;
        }
        return party.getClaimPartyOrganisationList().stream()
            .filter(claimPartyOrganisation -> claimPartyOrganisation.getActive() == YesOrNo.YES)
            .map(ClaimPartyOrganisationEntity::getOrganisation)
            .findFirst()
            .map(legalRepOrg -> OrganisationDetails.builder()
                .name(legalRepOrg.getOrganisationName())
                .organisationType(PARTY_TYPE_ORGANISATION)
                .cftOrganisationID(legalRepOrg.getOrganisationId())
                .build())
            .orElse(null);
    }

    private static List<String> contactList(String value) {
        return value == null || value.isBlank() ? null : List.of(value.strip());
    }

    private int numberOfPhysicalAttendees(PcsCaseEntity pcsCase) {
        return Math.toIntExact(activeParties(pcsCase).count()) + DUTY_SOLICITOR_ATTENDEES;
    }

    private CaseFlags caseFlags(PcsCaseEntity pcsCase, List<PartyDetails> parties) {
        Map<String, String> partyNames = new HashMap<>();
        parties.forEach(party -> partyNames.put(party.getPartyID(), partyName(party)));

        List<PartyFlags> flags = new ArrayList<>();
        activeFlags(pcsCase.getCaseFlags())
            .map(flag -> partyFlag(flag, null, CASE_LEVEL_FLAGS_PARTY_NAME))
            .forEach(flags::add);
        activeParties(pcsCase).forEach(party -> {
            String partyId = party.getId().toString();
            activeFlags(party.getDefendantFlags())
                .map(flag -> partyFlag(flag, partyId, partyNames.get(partyId)))
                .forEach(flags::add);
        });

        if (flags.isEmpty()) {
            return null;
        }
        return CaseFlags.builder()
            .flags(flags)
            .flagAmendUrl(String.format(CASE_FLAGS_PATH, pcsCase.getCaseReference()))
            .build();
    }

    private static Stream<? extends BaseCaseFlag> activeFlags(List<? extends BaseCaseFlag> flags) {
        return flags.stream()
            .filter(Objects::nonNull)
            .filter(flag -> ACTIVE_FLAG_STATUS.equals(flag.getDefaultStatus()))
            .filter(flag -> flag.getFlagRefData() != null);
    }

    private static PartyFlags partyFlag(BaseCaseFlag flag, String partyId, String partyName) {
        return PartyFlags.builder()
            .partyID(partyId)
            .partyName(partyName)
            .flagId(flag.getFlagRefData().getFlagCode())
            .flagDescription(flagDescription(flag))
            .flagStatus(flag.getDefaultStatus())
            .flagComment(flag.getFlagComment())
            .dateTimeCreated(flag.getDateTimeCreated())
            .dateTimeModified(flag.getDateTimeModified())
            .build();
    }

    // ExUI shows the description under an interpreter flag's name, so it carries the language
    private static String flagDescription(BaseCaseFlag flag) {
        String flagCode = flag.getFlagRefData().getFlagCode();
        boolean interpreter = IndividualDetailsFlagMapper.SPOKEN_LANGUAGE_INTERPRETER.equals(flagCode)
            || IndividualDetailsFlagMapper.SIGN_LANGUAGE_INTERPRETER.equals(flagCode);
        if (interpreter && flag.getSubTypeValue() != null && !flag.getSubTypeValue().isBlank()) {
            return flag.getSubTypeValue();
        }
        return FlagText.name(flag);
    }

    private static String partyName(PartyDetails party) {
        IndividualDetails individual = party.getIndividualDetails();
        if (individual == null) {
            return party.getOrganisationDetails().getName();
        }
        return Stream.of(individual.getFirstName(), individual.getLastName())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "));
    }

    private String listingComments(PcsCaseEntity pcsCase) {
        List<FlagText> flags = pcsCase.getCaseFlags().stream()
            .filter(flag -> ACTIVE_FLAG_STATUS.equals(flag.getDefaultStatus()))
            .filter(flag -> flag.getFlagRefData() != null)
            .map(FlagText::of)
            .toList();

        if (flags.isEmpty()) {
            return null;
        }

        String fullText = render(flags);
        if (fullText.length() <= MAX_LISTING_COMMENTS_LENGTH) {
            return fullText;
        }

        // text is over 2000 characters
        log.info("listingComments has been truncated for case {} ({} characters, limit {})",
                 pcsCase.getCaseReference(), fullText.length(), MAX_LISTING_COMMENTS_LENGTH);
        String fittedText = render(fitCommentsFairly(flags));
        // Only reachable if the flag names alone use up the limit; cut rather than have HMC reject the request
        return fittedText.length() > MAX_LISTING_COMMENTS_LENGTH
            ? fittedText.substring(0, MAX_LISTING_COMMENTS_LENGTH)
            : fittedText;
    }

    /**
     * Shortens the comments so the listing fits in {@value #MAX_LISTING_COMMENTS_LENGTH} characters,
     * keeping every flag name whole. The space left after is equally split between all comments.
     * Comments shorter than the new commentLength remain whole and comments longer than this limit will be truncated.
     */
    private static List<FlagText> fitCommentsFairly(List<FlagText> flags) {
        int commentedFlags = (int) flags.stream().filter(FlagText::hasComment).count();
        // max listing hsas been reached with just flag names
        if (commentedFlags == 0) {
            return flags;
        }
        int fixedLength = render(flags.stream().map(FlagText::withoutComment).toList()).length()
            + (commentedFlags * (COMMENT_OPEN.length() + COMMENT_CLOSE.length()));
        int remaining = MAX_LISTING_COMMENTS_LENGTH - fixedLength;

        int commentLength = Math.max(0,remaining / commentedFlags);

        List<FlagText> fitted = new ArrayList<>(flags);
        for (int i = 0; i < fitted.size(); i++) {
            FlagText flag = fitted.get(i);
            if (flag.hasComment()) {
                fitted.set(i, flag.withComment(shorten(flag.comment(), commentLength)));
            }
        }

        return fitted;
    }

    /**
     * Cuts a comment to {@code maxLength} characters, replacing the last 3 with {@value #ELLIPSIS} so the
     * listing officer knows there is more. Returns null (show the flag without comments) when there isn't
     * room for any text before the ellipsis.
     */
    static String shorten(String comment, int maxLength) {
        if (comment.length() <= maxLength) {
            return comment;
        }
        int limit = maxLength - ELLIPSIS.length();
        if (limit <= 0) {
            return null;
        }
        return comment.substring(0, limit) + ELLIPSIS;
    }

    private static String render(List<FlagText> flags) {
        return flags.stream()
            .map(FlagText::format)
            .collect(Collectors.joining(FLAG_SEPARATOR, CASE_FLAGS_PREFIX, ""));
    }

    /**
     * A flag's name and comment, trimmed. A comment that is missing, empty or only whitespace is held
     * as null, meaning the flag is listed without comments.
     */
    private record FlagText(String name, String comment) {

        static FlagText of(BaseCaseFlag flag) {
            String comment = flag.getFlagComment() == null ? "" : flag.getFlagComment().strip();
            return new FlagText(name(flag), comment.isEmpty() ? null : comment);
        }

        // An "Other" flag's real name is the description the user entered, not "Other"
        private static String name(BaseCaseFlag flag) {
            String otherDescription = Objects.toString(flag.getOtherDescription(), "").strip();
            if (OTHER_FLAG_CODE.equals(flag.getFlagRefData().getFlagCode()) && !otherDescription.isEmpty()) {
                return otherDescription;
            }
            return Objects.toString(flag.getFlagRefData().getFlagName(), "").strip();
        }

        boolean hasComment() {
            return comment != null;
        }

        FlagText withComment(String newComment) {
            return new FlagText(name, newComment);
        }

        FlagText withoutComment() {
            return withComment(null);
        }

        String format() {
            return hasComment() ? name + COMMENT_OPEN + comment + COMMENT_CLOSE : name;
        }
    }
}
