package uk.gov.hmcts.reform.pcs.hearings.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.hearings.config.ServiceHearingValuesConfiguration;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Builds the serviceHearingValues payload that ExUI requests when a user starts a hearing request.
 * Values that are the same for every Possessions case come from {@link ServiceHearingValuesConfiguration};
 * values that depend on the case are then derived from PCS data.
 */
@Slf4j
@Service
public class ServiceHearingService {

    static final int DUTY_SOLICITOR_ATTENDEES = 1;
    static final String CASE_FLAGS_PREFIX = "Case Flags: ";
    private static final String CASE_FILE_VIEW_PATH = "/cases/case-details/PCS/PCS/%d#Case%%20File%%20View";

    private final ServiceHearingValuesConfiguration defaults;
    private final PcsCaseRepository pcsCaseRepository;
    private final CaseNameFormatter caseNameFormatter;
    private final String manageCaseUrl;

    public ServiceHearingService(ServiceHearingValuesConfiguration defaults,
                                 PcsCaseRepository pcsCaseRepository,
                                 CaseNameFormatter caseNameFormatter,
                                 @Value("${manage_case.url}") String manageCaseUrl) {
        this.defaults = defaults;
        this.pcsCaseRepository = pcsCaseRepository;
        this.caseNameFormatter = caseNameFormatter;
        this.manageCaseUrl = manageCaseUrl;
    }

    @Transactional(readOnly = true)
    public ServiceHearingValues getServiceHearingValues(long caseReference) {
        PcsCaseEntity pcsCase = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        ServiceHearingValues values = defaults.getDefaultValues(String.valueOf(caseReference));

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

    private String caseDeepLink(long caseReference) {
        return manageCaseUrl + String.format(CASE_FILE_VIEW_PATH, caseReference);
    }

    private LocalDate caseSlaStartDate(PcsCaseEntity pcsCase) {
        return pcsCase.getClaims().stream()
            .findFirst()
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
        List<String> flags = pcsCase.getCaseFlags().stream()
            .filter(flag -> flag.getFlagRefData() != null)
            .sorted(Comparator.comparing(BaseCaseFlag::getDateTimeCreated,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .map(ServiceHearingService::formatFlag)
            .toList();

        return flags.isEmpty() ? null : CASE_FLAGS_PREFIX + String.join(", ", flags);
    }

    private static String formatFlag(BaseCaseFlag flag) {
        String name = flag.getFlagRefData().getFlagName();
        String comment = flag.getFlagComment();
        return comment == null || comment.isBlank()
            ? name
            : name + " (Comments: " + comment + ")";
    }
}
