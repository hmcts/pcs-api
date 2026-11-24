package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.pcs.ccd.view.CaseDetailsTabUtil.NO_ANSWER;

@ExtendWith(MockitoExtension.class)
class CitizenClaimTabDetailsBuilderTest {

    @Mock
    private FeatureToggleService featureToggleService;

    private CitizenClaimTabDetailsBuilder underTest;
    private PcsCaseEntity pcsCaseEntity;
    private ClaimEntity claim;

    @BeforeEach
    void setUp() {
        underTest = new CitizenClaimTabDetailsBuilder(featureToggleService);
        pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setLegislativeCountry(LegislativeCountry.ENGLAND);
        claim = ClaimEntity.builder().claimantType(ClaimantType.PRIVATE_LANDLORD).build();
        pcsCaseEntity.setClaims(new ArrayList<>(List.of(claim)));
    }

    private void flag(boolean enabled) {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM))
            .thenReturn(enabled);
    }

    private NoticeOfPossessionEntity servedByPostAndEmail() {
        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeServed(YesOrNo.YES)
            .noticeType("Notice seeking possession")
            .servingMethod(NoticeServiceMethod.FIRST_CLASS_POST)
            .noticeDate(LocalDate.of(2026, 8, 1))
            .build();
        notice.addServiceMethod(NoticeServiceMethodEntity.builder()
                                    .method(NoticeServiceMethod.EMAIL).servedDate(LocalDate.of(2026, 8, 3))
                                    .details("tom@example.com").displayOrder(2).build());
        notice.addServiceMethod(NoticeServiceMethodEntity.builder()
                                    .method(NoticeServiceMethod.FIRST_CLASS_POST)
                                    .servedDate(LocalDate.of(2026, 8, 1)).displayOrder(1).build());
        claim.setNoticeOfPossession(notice);
        return notice;
    }

    @Test
    void shouldListEveryNoticeMethodAndTheEnglandNoticeType() {
        flag(true);
        servedByPostAndEmail();
        NoticeTabDetails tab = NoticeTabDetails.builder().noticeServed("Yes").noticeDate("1 August 2026").build();

        underTest.addNoticeDetails(tab, pcsCaseEntity);

        assertThat(tab.getTypeOfNoticeServed()).isEqualTo("Notice seeking possession");
        assertThat(tab.getNoticeServiceMethods())
            .extracting(ListValue::getValue)
            .extracting(NoticeServiceMethodTabDetails::getMethod, NoticeServiceMethodTabDetails::getDateServed,
                        NoticeServiceMethodTabDetails::getDetails)
            .containsExactly(
                tuple(NoticeServiceMethod.FIRST_CLASS_POST.getLabel(), "1 August 2026", null),
                tuple(NoticeServiceMethod.EMAIL.getLabel(), "3 August 2026", "tom@example.com"));
    }

    @Test
    void shouldShowTheNoticeDateOfAMethodExUiRecordsATimeFor() {
        flag(true);
        claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                                        .noticeServed(YesOrNo.YES)
                                        .servingMethod(NoticeServiceMethod.EMAIL)
                                        .noticeDate(LocalDate.of(2026, 8, 3))
                                        .noticeDetails("tom@example.com")
                                        .build());
        NoticeTabDetails tab = NoticeTabDetails.builder()
            .noticeServed("Yes").noticeMethod(NoticeServiceMethod.EMAIL.getLabel()).noticeDate(NO_ANSWER).build();

        underTest.addNoticeDetails(tab, pcsCaseEntity);

        assertThat(tab.getNoticeDate()).isEqualTo("3 August 2026");
    }

    @Test
    void shouldKeepTheDateAndTimeExUiRecorded() {
        flag(true);
        claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                                        .noticeServed(YesOrNo.YES)
                                        .servingMethod(NoticeServiceMethod.EMAIL)
                                        .noticeDateTime(LocalDateTime.of(2026, 8, 3, 10, 30))
                                        .build());
        NoticeTabDetails tab = NoticeTabDetails.builder().noticeServed("Yes").noticeDate("3 August 2026, 10:30:00AM")
            .build();

        underTest.addNoticeDetails(tab, pcsCaseEntity);

        assertThat(tab.getNoticeDate()).isEqualTo("3 August 2026, 10:30:00AM");
        assertThat(tab.getNoticeServiceMethods()).isNull();
        assertThat(tab.getTypeOfNoticeServed()).isNull();
    }

    @Test
    void shouldShowWhyNoticeWasNotServed() {
        flag(true);
        claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                                        .noticeServed(YesOrNo.NO).noticeStatement("No notice is needed").build());
        NoticeTabDetails tab = NoticeTabDetails.builder().noticeServed("No").build();

        underTest.addNoticeDetails(tab, pcsCaseEntity);

        assertThat(tab.getStatement()).isEqualTo("No notice is needed");
    }

    @Test
    void shouldAddTheRentAnswers() {
        flag(true);
        servedByPostAndEmail();
        claim.setRentArrears(RentArrearsEntity.builder()
                                 .totalRentArrears(new BigDecimal("5000.00"))
                                 .totalRentArrearsDate(LocalDate.of(2026, 11, 1))
                                 .arrearsAtNoticeDate(new BigDecimal("3750.50"))
                                 .proofType(CitizenClaimRentProofType.TABLE)
                                 .build());
        TenancyLicenceEntity tenancy = new TenancyLicenceEntity();
        tenancy.setRentDueDay(15);
        pcsCaseEntity.setTenancyLicence(tenancy);
        RentArrearsTabDetails tab = RentArrearsTabDetails.builder().arrearsTotal("£5000").build();

        underTest.addRentArrearsDetails(tab, pcsCaseEntity);

        assertThat(tab.getRentDueDay()).isEqualTo("15");
        assertThat(tab.getArrearsAtNoticeDate()).isEqualTo("£3750.50");
        assertThat(tab.getArrearsAtNoticeServedDate()).isEqualTo("1 August 2026");
        assertThat(tab.getArrearsTotalDate()).isEqualTo("1 November 2026");
        assertThat(tab.getProofType()).isEqualTo(CitizenClaimRentProofType.TABLE.getLabel());
    }

    @Test
    void shouldAddTheNoticeMethodsAndArrearsToTheSummary() {
        flag(true);
        servedByPostAndEmail();
        claim.setRentArrears(RentArrearsEntity.builder()
                                 .totalRentArrears(new BigDecimal("5000"))
                                 .totalRentArrearsDate(LocalDate.of(2026, 11, 1))
                                 .arrearsAtNoticeDate(new BigDecimal("3750"))
                                 .build());
        SummaryTab summary = SummaryTab.builder()
            .rentArrearsDetails(RentArrearsTabDetails.builder().arrearsTotal("£5000").build())
            .noticeDetails(uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.NoticeTabDetails.builder()
                               .noticeServedDate("01/08/2026").build())
            .build();

        underTest.addSummaryDetails(summary, pcsCaseEntity);

        assertThat(summary.getNoticeDetails().getNoticeMethod()).isEqualTo(
            NoticeServiceMethod.FIRST_CLASS_POST.getLabel() + ", " + NoticeServiceMethod.EMAIL.getLabel());
        assertThat(summary.getNoticeDetails().getNoticeServedDate()).isEqualTo("01/08/2026");
        assertThat(summary.getRentArrearsDetails().getArrearsAtNoticeDate()).isEqualTo("£3750");
        assertThat(summary.getRentArrearsDetails().getArrearsAtNoticeServedDate()).isEqualTo("01/08/2026");
        assertThat(summary.getRentArrearsDetails().getArrearsTotalDate()).isEqualTo("01/11/2026");
        assertThat(summary.getRentArrearsDetails().getProofType()).isNull();
    }

    @Test
    void shouldGiveTheSummaryTheNoticeDateWhenItHasNone() {
        flag(true);
        claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                                        .noticeServed(YesOrNo.YES)
                                        .servingMethod(NoticeServiceMethod.OTHER)
                                        .noticeDate(LocalDate.of(2026, 8, 4))
                                        .build());
        SummaryTab summary = SummaryTab.builder().build();

        underTest.addSummaryDetails(summary, pcsCaseEntity);

        assertThat(summary.getNoticeDetails().getNoticeServedDate()).isEqualTo("04/08/2026");
        assertThat(summary.getNoticeDetails().getNoticeMethod()).isNull();
    }

    @Test
    void shouldChangeNothingWithTheFlagOff() {
        flag(false);
        servedByPostAndEmail();
        claim.setRentArrears(RentArrearsEntity.builder().arrearsAtNoticeDate(new BigDecimal("3750")).build());
        NoticeTabDetails notice = NoticeTabDetails.builder().noticeServed("Yes").noticeDate(NO_ANSWER).build();
        RentArrearsTabDetails rent = RentArrearsTabDetails.builder().arrearsTotal("£5000").build();
        SummaryTab summary = SummaryTab.builder().rentArrearsDetails(RentArrearsTabDetails.builder().build()).build();

        underTest.addNoticeDetails(notice, pcsCaseEntity);
        underTest.addRentArrearsDetails(rent, pcsCaseEntity);
        underTest.addSummaryDetails(summary, pcsCaseEntity);

        assertThat(notice).isEqualTo(NoticeTabDetails.builder().noticeServed("Yes").noticeDate(NO_ANSWER).build());
        assertThat(rent).isEqualTo(RentArrearsTabDetails.builder().arrearsTotal("£5000").build());
        assertThat(summary).isEqualTo(SummaryTab.builder()
                                          .rentArrearsDetails(RentArrearsTabDetails.builder().build()).build());
    }

    @Test
    void shouldChangeNothingForAWalesCaseNotMadeByAPrivateLandlord() {
        flag(true);
        pcsCaseEntity.setLegislativeCountry(LegislativeCountry.WALES);
        claim.setClaimantType(ClaimantType.COMMUNITY_LANDLORD);
        servedByPostAndEmail();
        NoticeTabDetails notice = NoticeTabDetails.builder().noticeServed("Yes").build();
        SummaryTab summary = SummaryTab.builder().build();

        underTest.addNoticeDetails(notice, pcsCaseEntity);
        underTest.addSummaryDetails(summary, pcsCaseEntity);

        assertThat(notice).isEqualTo(NoticeTabDetails.builder().noticeServed("Yes").build());
        assertThat(summary).isEqualTo(SummaryTab.builder().build());
    }

    @Test
    void shouldAddTheCitizenAnswersToAWalesCaseMadeByAPrivateLandlord() {
        flag(true);
        pcsCaseEntity.setLegislativeCountry(LegislativeCountry.WALES);
        servedByPostAndEmail();
        SummaryTab summary = SummaryTab.builder().build();

        underTest.addSummaryDetails(summary, pcsCaseEntity);

        assertThat(summary.getNoticeDetails().getNoticeMethod())
            .isEqualTo(NoticeServiceMethod.FIRST_CLASS_POST.getLabel() + ", " + NoticeServiceMethod.EMAIL.getLabel());
    }

    @Test
    void shouldCopeWithACaseWithoutAClaim() {
        flag(true);
        pcsCaseEntity.setClaims(new ArrayList<>());
        NoticeTabDetails notice = NoticeTabDetails.builder().noticeServed("Yes").build();
        RentArrearsTabDetails rent = RentArrearsTabDetails.builder().build();
        SummaryTab summary = SummaryTab.builder().build();

        underTest.addNoticeDetails(notice, pcsCaseEntity);
        underTest.addRentArrearsDetails(rent, pcsCaseEntity);
        underTest.addSummaryDetails(summary, pcsCaseEntity);

        assertThat(notice).isEqualTo(NoticeTabDetails.builder().noticeServed("Yes").build());
        assertThat(rent).isEqualTo(RentArrearsTabDetails.builder().build());
        assertThat(summary).isEqualTo(SummaryTab.builder().build());
    }
}
