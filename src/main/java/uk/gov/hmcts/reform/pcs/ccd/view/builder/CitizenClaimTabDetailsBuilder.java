package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentProofType;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeServiceMethodTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeServiceMethodEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.view.CaseDetailsTabUtil.DATE_FORMATTER;

/**
 * Adds to the tabs the citizen claim answers that the ExUI claim does not record: every method a notice
 * was served by, an England notice type or reason for not serving notice, the date of a notice served by a
 * method ExUI records a time for, the rent due day, the arrears on the date notice was served and how the
 * arrears are proved. Behind the private landlord make a claim flag, and England only.
 */
@Component
@RequiredArgsConstructor
public class CitizenClaimTabDetailsBuilder {

    private static final DateTimeFormatter SUMMARY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FeatureToggleService featureToggleService;

    public void addNoticeDetails(NoticeTabDetails noticeTabDetails, PcsCaseEntity pcsCaseEntity) {
        Optional<NoticeOfPossessionEntity> notice = mainClaim(pcsCaseEntity)
            .map(ClaimEntity::getNoticeOfPossession);
        if (noticeTabDetails == null || notice.isEmpty() || !applies(pcsCaseEntity)) {
            return;
        }
        NoticeOfPossessionEntity noticeEntity = notice.get();

        if (noticeEntity.getNoticeServed() == YesOrNo.NO) {
            if (StringUtils.isNotBlank(noticeEntity.getNoticeStatement())) {
                noticeTabDetails.setStatement(noticeEntity.getNoticeStatement());
            }
            return;
        }
        if (noticeEntity.getNoticeServed() != YesOrNo.YES) {
            return;
        }

        if (StringUtils.isNotBlank(noticeEntity.getNoticeType())) {
            noticeTabDetails.setTypeOfNoticeServed(noticeEntity.getNoticeType());
        }
        if (noticeEntity.getNoticeDateTime() == null && noticeEntity.getNoticeDate() != null) {
            noticeTabDetails.setNoticeDate(noticeEntity.getNoticeDate().format(DATE_FORMATTER));
        }

        List<ListValue<NoticeServiceMethodTabDetails>> methods = serviceMethods(noticeEntity).stream()
            .map(method -> ListValue.<NoticeServiceMethodTabDetails>builder()
                .id(method.getId() != null ? method.getId().toString() : null)
                .value(NoticeServiceMethodTabDetails.builder()
                           .method(method.getMethod().getLabel())
                           .dateServed(format(method.getServedDate(), DATE_FORMATTER))
                           .details(method.getDetails())
                           .build())
                .build())
            .toList();
        if (!methods.isEmpty()) {
            noticeTabDetails.setNoticeServiceMethods(methods);
        }
    }

    public void addRentArrearsDetails(RentArrearsTabDetails rentArrearsTabDetails, PcsCaseEntity pcsCaseEntity) {
        if (rentArrearsTabDetails == null || !applies(pcsCaseEntity)) {
            return;
        }
        TenancyLicenceEntity tenancyLicence = pcsCaseEntity.getTenancyLicence();
        if (tenancyLicence != null && tenancyLicence.getRentDueDay() != null) {
            rentArrearsTabDetails.setRentDueDay(tenancyLicence.getRentDueDay().toString());
        }
        addArrearsFigures(rentArrearsTabDetails, pcsCaseEntity, DATE_FORMATTER);
        mainClaim(pcsCaseEntity)
            .map(ClaimEntity::getRentArrears)
            .map(RentArrearsEntity::getProofType)
            .map(CitizenClaimRentProofType::getLabel)
            .ifPresent(rentArrearsTabDetails::setProofType);
    }

    public void addSummaryDetails(SummaryTab summaryTab, PcsCaseEntity pcsCaseEntity) {
        if (summaryTab == null || !applies(pcsCaseEntity)) {
            return;
        }
        if (summaryTab.getRentArrearsDetails() != null) {
            addArrearsFigures(summaryTab.getRentArrearsDetails(), pcsCaseEntity, SUMMARY_DATE_FORMATTER);
        }

        Optional<NoticeOfPossessionEntity> notice = mainClaim(pcsCaseEntity)
            .map(ClaimEntity::getNoticeOfPossession)
            .filter(noticeEntity -> noticeEntity.getNoticeServed() == YesOrNo.YES);
        if (notice.isEmpty()) {
            return;
        }
        NoticeOfPossessionEntity noticeEntity = notice.get();
        String methods = serviceMethods(noticeEntity).stream()
            .map(method -> method.getMethod().getLabel())
            .collect(Collectors.joining(", "));
        String servedDate = noticeEntity.getNoticeDateTime() == null
            ? format(noticeEntity.getNoticeDate(), SUMMARY_DATE_FORMATTER) : null;
        if (methods.isEmpty() && servedDate == null) {
            return;
        }

        uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.NoticeTabDetails summaryNotice =
            Optional.ofNullable(summaryTab.getNoticeDetails())
                .orElseGet(uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.NoticeTabDetails::new);
        if (!methods.isEmpty()) {
            summaryNotice.setNoticeMethod(methods);
        }
        if (summaryNotice.getNoticeServedDate() == null && servedDate != null) {
            summaryNotice.setNoticeServedDate(servedDate);
        }
        summaryTab.setNoticeDetails(summaryNotice);
    }

    /**
     * Whether the citizen claim answers are added: the flag is on and the case is in England, the only
     * country citizen claims are made for.
     */
    public boolean applies(PcsCaseEntity pcsCaseEntity) {
        return featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)
            && pcsCaseEntity != null
            && pcsCaseEntity.getLegislativeCountry() != LegislativeCountry.WALES;
    }

    private void addArrearsFigures(RentArrearsTabDetails rentArrearsTabDetails, PcsCaseEntity pcsCaseEntity,
                                   DateTimeFormatter dateFormatter) {
        Optional<ClaimEntity> claim = mainClaim(pcsCaseEntity);
        Optional<RentArrearsEntity> rentArrears = claim.map(ClaimEntity::getRentArrears);
        if (rentArrears.isEmpty()) {
            return;
        }
        RentArrearsEntity rentArrearsEntity = rentArrears.get();

        if (rentArrearsEntity.getTotalRentArrearsDate() != null) {
            rentArrearsTabDetails.setArrearsTotalDate(format(rentArrearsEntity.getTotalRentArrearsDate(),
                                                             dateFormatter));
        }
        if (rentArrearsEntity.getArrearsAtNoticeDate() != null) {
            rentArrearsTabDetails.setArrearsAtNoticeDate(formatMoney(rentArrearsEntity.getArrearsAtNoticeDate()));
            claim.map(ClaimEntity::getNoticeOfPossession)
                .map(NoticeOfPossessionEntity::getNoticeDate)
                .ifPresent(date -> rentArrearsTabDetails.setArrearsAtNoticeServedDate(format(date, dateFormatter)));
        }
    }

    private static List<NoticeServiceMethodEntity> serviceMethods(NoticeOfPossessionEntity notice) {
        if (notice.getServiceMethods() == null) {
            return List.of();
        }
        return notice.getServiceMethods().stream()
            .filter(method -> method.getMethod() != null)
            .sorted(Comparator.comparingInt(NoticeServiceMethodEntity::getDisplayOrder))
            .toList();
    }

    private static Optional<ClaimEntity> mainClaim(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity == null || pcsCaseEntity.getClaims() == null) {
            return Optional.empty();
        }
        return pcsCaseEntity.getClaims().stream().findFirst();
    }

    private static String format(LocalDate date, DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : null;
    }

    private static String formatMoney(BigDecimal amount) {
        BigDecimal value = amount.stripTrailingZeros().scale() <= 0 ? amount.stripTrailingZeros() : amount;
        return "£" + value.toPlainString();
    }
}
