package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class MortgageClaimViewsTest {

    @Mock
    private UploadTimestampProvider uploadTimestampProvider;

    private PcsCaseEntity mortgageCase;
    private PCSCase pcsCase;

    @BeforeEach
    void setUp() {
        mortgageCase = new PcsCaseEntity();
        mortgageCase.setLegislativeCountry(LegislativeCountry.ENGLAND);
        ClaimEntity claim = ClaimEntity.builder()
            .pcsCase(mortgageCase)
            .claimantType(ClaimantType.MORTGAGE_LENDER)
            .build();
        mortgageCase.setClaims(new ArrayList<>(List.of(claim)));
        pcsCase = PCSCase.builder().legislativeCountry(LegislativeCountry.ENGLAND).build();
    }

    @Test
    void tenancyLicenceViewLeavesTenancyFieldsEmpty() {
        new TenancyLicenceView(uploadTimestampProvider).setCaseFields(pcsCase, mortgageCase);

        assertThat(pcsCase.getTenancyLicenceDetails()).isNull();
        assertThat(pcsCase.getOccupationLicenceDetailsWales()).isNull();
    }

    @Test
    void tenancyLicenceViewLeavesOccupationFieldsEmptyInWales() {
        mortgageCase.setLegislativeCountry(LegislativeCountry.WALES);
        pcsCase.setLegislativeCountry(LegislativeCountry.WALES);

        new TenancyLicenceView(uploadTimestampProvider).setCaseFields(pcsCase, mortgageCase);

        assertThat(pcsCase.getOccupationLicenceDetailsWales()).isNull();
    }

    @Test
    void rentDetailsViewLeavesRentFieldsEmpty() {
        new RentDetailsView().setCaseFields(pcsCase, mortgageCase);

        assertThat(pcsCase.getRentDetails()).isNull();
        assertThat(pcsCase.getShowRentSectionPage()).isNull();
    }

    @Test
    void claimGroundsViewLeavesNoGroundSummaries() {
        new ClaimGroundsView().setCaseFields(pcsCase, mortgageCase);

        assertThat(pcsCase.getClaimGroundSummaries()).isEmpty();
    }

    @Test
    void rentArrearsViewLeavesRentArrearsEmpty() {
        assertThatCode(() -> new RentArrearsView(uploadTimestampProvider).setCaseFields(pcsCase, mortgageCase))
            .doesNotThrowAnyException();

        assertThat(pcsCase.getRentArrears()).isNull();
        assertThat(pcsCase.getArrearsJudgmentWanted()).isNull();
    }

    @Test
    void noticeOfPossessionViewLeavesNoticeFieldsEmpty() {
        new NoticeOfPossessionView().setCaseFields(pcsCase, mortgageCase);

        assertThat(pcsCase.getNoticeServed()).isNull();
        assertThat(pcsCase.getNoticeServedDetails()).isNull();
        assertThat(pcsCase.getWalesNoticeDetails()).isNull();
    }
}
