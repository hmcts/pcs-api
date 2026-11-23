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
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    static final int MAX_LISTING_COMMENTS_LENGTH = 2000;
    static final int MAX_COMMENT_LENGTH = 200;
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
        String fittedText = render(fitCommentsFairly(flags));
        // Only reachable if the flag names alone use up the limit; cut rather than have HMC reject the request
        return fittedText.length() > MAX_LISTING_COMMENTS_LENGTH
            ? fittedText.substring(0, MAX_LISTING_COMMENTS_LENGTH)
            : fittedText;
    }

    /**
     * Shortens the comments so the listing fits in {@value #MAX_LISTING_COMMENTS_LENGTH} characters,
     * keeping every flag name whole. The space left after the fixed text is shared out smallest
     * comment first: each comment gets the lesser of its own length (at most
     * {@value #MAX_COMMENT_LENGTH}) and an equal share of what is still unallocated. Short comments
     * therefore stay whole, and the space they don't use goes to the longer ones.
     */
    private static List<FlagText> fitCommentsFairly(List<FlagText> flags) {
        int commentedFlags = (int) flags.stream().filter(FlagText::hasComment).count();
        int fixedLength = render(flags.stream().map(FlagText::withoutComment).toList()).length()
            + (commentedFlags * (COMMENT_OPEN.length() + COMMENT_CLOSE.length()));
        int remaining = MAX_LISTING_COMMENTS_LENGTH - fixedLength;

        List<Integer> smallestFirst = IntStream.range(0, flags.size())
            .filter(i -> flags.get(i).hasComment())
            .boxed()
            .sorted(Comparator.comparingInt(i -> commentDemand(flags.get(i))))
            .toList();

        List<FlagText> fitted = new ArrayList<>(flags);
        int unallocated = smallestFirst.size();
        for (int i : smallestFirst) {
            FlagText flag = flags.get(i);
            int share = Math.max(0, remaining) / unallocated--;
            int allowance = Math.min(commentDemand(flag), share);
            fitted.set(i, flag.withComment(shorten(flag.comment(), allowance)));
            remaining -= allowance;
        }
        return fitted;
    }

    private static int commentDemand(FlagText flag) {
        return Math.min(flag.comment().length(), MAX_COMMENT_LENGTH);
    }

    /**
     * Cuts a comment to at most {@code maxLength} characters, ending it with {@value #ELLIPSIS} so the
     * listing officer knows there is more. Cuts at the last whitespace where that keeps at least half
     * the space, otherwise mid-word. Returns null (show the flag without comments) when there isn't
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
        int cut = lastWhitespaceAtOrBefore(comment, limit);
        if (cut < limit / 2) {
            cut = limit;
        }
        return comment.substring(0, cut).stripTrailing() + ELLIPSIS;
    }

    private static int lastWhitespaceAtOrBefore(String text, int index) {
        for (int i = Math.min(index, text.length() - 1); i >= 0; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
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
            return new FlagText(
                Objects.toString(flag.getFlagRefData().getFlagName(), "").strip(),
                comment.isEmpty() ? null : comment);
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
