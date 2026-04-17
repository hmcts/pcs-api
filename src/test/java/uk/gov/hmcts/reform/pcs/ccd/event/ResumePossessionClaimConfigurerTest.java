package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AdditionalReasonsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AlternativesToPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AssuredNoArrearsGroundsForPossessionPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantInformationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CompletingYourClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DailyRentAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DemotionOfTenancyHousingActOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DemotionOfTenancyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOrOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOtherGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.MediationAndSettlement;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.MoneyJudgment;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoRentArrearsGroundsForPossessionReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.PreActionProtocol;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrears;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundForPossessionAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossessionPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsOrBreachOfTenancyGround;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.StatementOfExpressTerms;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyHousingActOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionToBuyDemotionOfTenancyActs;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionToBuyDemotionOfTenancyOrderReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.TenancyLicenceDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeOrMortgageeDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeOrMortgageeEntitledToClaimRelief;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UploadAdditionalDocumentsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WalesCheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WantToUploadDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.GroundsForPossessionWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.OccupationLicenceDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ReasonsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.SecureContractGroundsForPossessionWalesPage;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.PageConfigurerHelper.verifyAndCount;

@ExtendWith(MockitoExtension.class)
public class ResumePossessionClaimConfigurerTest {

    @InjectMocks
    private ResumePossessionClaimConfigurer underTest;

    @Mock
    private ResumeClaim resumeClaim;
    @Mock
    private SelectClaimantType selectClaimantType;
    @Mock
    private NoticeDetails noticeDetails;
    @Mock
    private UploadAdditionalDocumentsDetails uploadAdditionalDocumentsDetails;
    @Mock
    private TenancyLicenceDetailsPage tenancyLicenceDetails;
    @Mock
    private ContactPreferences contactPreferences;
    @Mock
    private DefendantsDetails defendantsDetails;
    @Mock
    private NoRentArrearsGroundsForPossessionReason noRentArrearsGroundsForPossessionReason;
    @Mock
    private AdditionalReasonsForPossession additionalReasonsForPossession;
    @Mock
    private SecureOrFlexibleGroundsForPossessionReasons secureOrFlexibleGroundsForPossessionReasons;
    @Mock
    private MediationAndSettlement mediationAndSettlement;
    @Mock
    private ClaimantCircumstancesPage claimantCircumstancesPage;
    @Mock
    private IntroductoryDemotedOtherGroundsReasons introductoryDemotedOtherGroundsReasons;
    @Mock
    private IntroductoryDemotedOrOtherGroundsForPossession introductoryDemotedOrOtherGroundsForPossession;
    @Mock
    private RentArrearsGroundsForPossessionReasons rentArrearsGroundsForPossessionReasons;
    @Mock
    private SuspensionToBuyDemotionOfTenancyOrderReasons suspensionToBuyDemotionOfTenancyOrderReasons;
    @Mock
    private DefendantCircumstancesPage defendantCircumstancesPage;
    @Mock
    private SuspensionOfRightToBuyOrderReason suspensionOfRightToBuyOrderReason;
    @Mock
    private StatementOfExpressTerms statementOfExpressTerms;
    @Mock
    private DemotionOfTenancyOrderReason demotionOfTenancyOrderReason;
    @Mock
    private ClaimantInformationPage claimantInformationPage;
    @Mock
    private ClaimantDetailsWalesPage claimantDetailsWales;
    @Mock
    private ProhibitedConductWales prohibitedConductWalesPage;
    @Mock
    private OccupationLicenceDetailsWalesPage occupationLicenceDetailsWalesPage;
    @Mock
    private GroundsForPossessionWalesPage groundsForPossessionWales;
    @Mock
    private SecureContractGroundsForPossessionWalesPage secureContractGroundsForPossessionWales;
    @Mock
    private ReasonsForPossessionWales reasonsForPossessionWales;
    @Mock
    private RentArrearsGroundsForPossessionPage rentArrearsGroundsForPossessionPage;
    @Mock
    private RentArrearsGroundForPossessionAdditionalGrounds rentArrearsGroundForPossessionAdditionalGrounds;
    @Mock
    private AssuredNoArrearsGroundsForPossessionPage noRentArrearsGroundsForPossessionOptions;
    @Mock
    private CheckingNotice checkingNotice;
    @Mock
    private WalesCheckingNotice walesCheckingNotice;
    @Mock
    private ASBQuestionsWales asbQuestionsWales;
    @Mock
    private UnderlesseeOrMortgageeDetailsPage underlesseeOrMortgageeDetailsPage;
    @Mock
    private RentDetailsPage rentDetailsPage;

    @Test
    @SuppressWarnings("squid:S5961")
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        underTest.configurePages(pageBuilder);

        // Then
        ArgumentCaptor<CcdPageConfiguration> pageCaptor = ArgumentCaptor.forClass(CcdPageConfiguration.class);
        InOrder inOrder = inOrder(pageBuilder);
        Mockito.verify(pageBuilder, Mockito.atLeastOnce()).add(pageCaptor.capture());
        AtomicInteger verificationCount = new AtomicInteger(0);

        verifyAndCount(inOrder, pageBuilder, resumeClaim, verificationCount);
        verifyAndCount(inOrder, pageBuilder, selectClaimantType, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ClaimantTypeNotEligibleEngland.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ClaimantTypeNotEligibleWales.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, SelectClaimType.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ClaimTypeNotEligibleEngland.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ClaimTypeNotEligibleWales.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, claimantInformationPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, claimantDetailsWales, verificationCount);
        verifyAndCount(inOrder, pageBuilder, contactPreferences, verificationCount);
        verifyAndCount(inOrder, pageBuilder, defendantsDetails, verificationCount);
        verifyAndCount(inOrder, pageBuilder, tenancyLicenceDetails, verificationCount);
        verifyAndCount(inOrder, pageBuilder, occupationLicenceDetailsWalesPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, groundsForPossessionWales, verificationCount);
        verifyAndCount(inOrder, pageBuilder, secureContractGroundsForPossessionWales, verificationCount);
        verifyAndCount(inOrder, pageBuilder, reasonsForPossessionWales, verificationCount);
        verifyAndCount(inOrder, pageBuilder, asbQuestionsWales, verificationCount);
        verifyAndCount(inOrder, pageBuilder, SecureOrFlexibleGroundsForPossession.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, RentArrearsOrBreachOfTenancyGround.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, secureOrFlexibleGroundsForPossessionReasons, verificationCount);
        verifyAndCount(inOrder, pageBuilder, introductoryDemotedOrOtherGroundsForPossession, verificationCount);
        verifyAndCount(inOrder, pageBuilder, introductoryDemotedOtherGroundsReasons, verificationCount);
        verifyAndCount(inOrder, pageBuilder, GroundsForPossession.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, rentArrearsGroundsForPossessionPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, rentArrearsGroundForPossessionAdditionalGrounds, verificationCount);
        verifyAndCount(inOrder, pageBuilder, rentArrearsGroundsForPossessionReasons, verificationCount);
        verifyAndCount(inOrder, pageBuilder, noRentArrearsGroundsForPossessionOptions, verificationCount);
        verifyAndCount(inOrder, pageBuilder, noRentArrearsGroundsForPossessionReason, verificationCount);
        verifyAndCount(inOrder, pageBuilder, PreActionProtocol.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, mediationAndSettlement, verificationCount);
        verifyAndCount(inOrder, pageBuilder, checkingNotice, verificationCount);
        verifyAndCount(inOrder, pageBuilder, walesCheckingNotice, verificationCount);
        verifyAndCount(inOrder, pageBuilder, noticeDetails, verificationCount);
        verifyAndCount(inOrder, pageBuilder, rentDetailsPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, DailyRentAmount.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, RentArrears.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, MoneyJudgment.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, claimantCircumstancesPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, defendantCircumstancesPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, prohibitedConductWalesPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, AlternativesToPossessionOptions.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, SuspensionOfRightToBuyHousingActOptions.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, suspensionOfRightToBuyOrderReason, verificationCount);
        verifyAndCount(inOrder, pageBuilder, DemotionOfTenancyHousingActOptions.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, SuspensionToBuyDemotionOfTenancyActs.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, statementOfExpressTerms, verificationCount);
        verifyAndCount(inOrder, pageBuilder, demotionOfTenancyOrderReason, verificationCount);
        verifyAndCount(inOrder, pageBuilder, suspensionToBuyDemotionOfTenancyOrderReasons, verificationCount);
        verifyAndCount(inOrder, pageBuilder, additionalReasonsForPossession, verificationCount);
        verifyAndCount(inOrder, pageBuilder, UnderlesseeOrMortgageeEntitledToClaimRelief.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, underlesseeOrMortgageeDetailsPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, WantToUploadDocuments.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, uploadAdditionalDocumentsDetails, verificationCount);
        verifyAndCount(inOrder, pageBuilder, GeneralApplication.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, LanguageUsed.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, CompletingYourClaim.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, StatementOfTruth.class, verificationCount);

        int numberOfPages = pageCaptor.getAllValues().size();
        assertThat(verificationCount.get()).isEqualTo(numberOfPages);

        verifyNoMoreInteractions(pageBuilder);
    }

}
