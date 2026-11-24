package uk.gov.hmcts.reform.pcs.hearings.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.hearings.config.ServiceHearingValuesConfiguration;
import uk.gov.hmcts.reform.pcs.hearings.model.CaseCategory;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    static final String OTHER_FLAG_CODE = "OT0001";
    static final int MAX_LISTING_COMMENTS_LENGTH = 2000;
    static final String ELLIPSIS = "...";
    private static final String FLAG_SEPARATOR = ", ";
    private static final String COMMENT_OPEN = " (Comments: ";
    private static final String COMMENT_CLOSE = ")";
    private static final String CASE_FILE_VIEW_PATH = "/cases/case-details/%d#Case%%20File%%20View";

    private final ServiceHearingValuesConfiguration defaults;
    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final CaseNameFormatter caseNameFormatter;
    private final String manageCaseUrl;

    public ServiceHearingValuesMapper(ServiceHearingValuesConfiguration defaults,
                                      PcsCaseService pcsCaseService,
                                      PcsCaseRepository pcsCaseRepository,
                                      CaseNameFormatter caseNameFormatter,
                                      @Value("${manage_case.url}") String manageCaseUrl) {
        this.defaults = defaults;
        this.pcsCaseService = pcsCaseService;
        this.pcsCaseRepository = pcsCaseRepository;
        this.caseNameFormatter = caseNameFormatter;
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
        values.setCaseSlaStartDate(caseSlaStartDate(pcsCase));
        values.setNumberOfPhysicalAttendees(numberOfPhysicalAttendees(pcsCase));
        values.setListingComments(listingComments(pcsCase));
        values.setCaseCategories(caseCategories(pcsCase));

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

    private LocalDate caseSlaStartDate(PcsCaseEntity pcsCase) {
        return Optional.ofNullable(pcsCase.getMainClaim())
            .map(ClaimEntity::getClaimIssuedDate)
            .map(issued -> issued.atZone(ZoneOffset.UTC).withZoneSameInstant(UK_ZONE_ID).toLocalDate())
            .orElse(null);
    }

    private int numberOfPhysicalAttendees(PcsCaseEntity pcsCase) {
        long activeParties = pcsCase.getParties().stream()
            .filter(Objects::nonNull)
            .filter(party -> !party.isRemoved())
            .count();
        return Math.toIntExact(activeParties) + DUTY_SOLICITOR_ATTENDEES;
    }

    private List<CaseCategory> caseCategories(PcsCaseEntity pcsCase) {

        return List.of(
            CaseCategory.builder()
                .categoryType(defaults.getCaseTypeCategory())
                .categoryValue(pcsCase.getCaseTypeKey())
                .build(),
            CaseCategory.builder()
                .categoryType(defaults.getCaseSubTypeCategory())
                .categoryValue(pcsCase.getCaseSubtypeKey())
                .categoryParent(pcsCase.getCaseTypeKey())
                .build());
    }

    /**
     * Lists the case's active case-level flags for the listing officer, as
     * {@code Case Flags: name, name (Comments: comment)}, within HMC's 2000-character limit.
     *
     * <p>With fewer than {@value #FULL_TEXT_MAX_COMMENTED_FLAGS} commented flags the full text is tried
     * first and returned if it fits. Otherwise every flag name is kept in full and the comments are
     * shortened to share the space that is left (see {@link #fitCommentsFairly}).
     */

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
