package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.DELIVERED_PERMITTED_PLACE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.EMAIL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.FIRST_CLASS_POST;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.OTHER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.OTHER_ELECTRONIC;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.PERSONALLY_HANDED;

@ExtendWith(MockitoExtension.class)
class NoticeOfPossessionViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private ClaimEntity mainClaimEntity;
    @Mock(strictness = LENIENT)
    private NoticeOfPossessionEntity noticeOfPossessionEntity;
    @Captor
    private ArgumentCaptor<NoticeServedDetails> noticeServedDetailsCaptor;

    private NoticeOfPossessionView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getNoticeOfPossession()).thenReturn(noticeOfPossessionEntity);

        underTest = new NoticeOfPossessionView();
    }

    @Test
    void shouldNotSetAnythingIfNoMainClaim() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldNotSetAnythingIfNoNoticeOfPossession() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getNoticeOfPossession()).thenReturn(null);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldSetNoticeOfPossessionServedFlagForNonWales() {
        // Given
        when(noticeOfPossessionEntity.getNoticeServed()).thenReturn(YesOrNo.YES);
        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(EMAIL);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setNoticeServed(YesOrNo.YES);
    }

    @Test
    void shouldSetNoticeOfPossessionServedFlagAndTypeForWales() {
        // Given
        String noticeType = "Wales notice type";

        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.WALES);
        when(noticeOfPossessionEntity.getNoticeServed()).thenReturn(YesOrNo.YES);
        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(EMAIL);
        when(noticeOfPossessionEntity.getNoticeType()).thenReturn(noticeType);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<WalesNoticeDetails> walesNoticeDetailsCaptor = ArgumentCaptor.forClass(WalesNoticeDetails.class);
        verify(pcsCase).setWalesNoticeDetails(walesNoticeDetailsCaptor.capture());

        WalesNoticeDetails walesNoticeDetails = walesNoticeDetailsCaptor.getValue();
        assertThat(walesNoticeDetails.getNoticeServed()).isEqualTo(YesOrNo.YES);
        assertThat(walesNoticeDetails.getTypeOfNoticeServed()).isEqualTo(noticeType);
    }

    @Test
    void shouldIgnoreServingDatesWhenServingMethodIsNull() {
        // Given
        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(null);
        when(noticeOfPossessionEntity.getNoticeDate()).thenReturn(mock(LocalDate.class));
        when(noticeOfPossessionEntity.getNoticeDateTime()).thenReturn(mock(LocalDateTime.class));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setNoticeServedDetails(noticeServedDetailsCaptor.capture());

        NoticeServedDetails noticeServedDetails = noticeServedDetailsCaptor.getValue();

        assertThat(noticeServedDetails)
            .usingRecursiveComparison()
            .isEqualTo(NoticeServedDetails.builder().build());
    }

    @Test
    void shouldSetNoticeServedDateForPost() {
        // Given
        LocalDate postedDate = mock(LocalDate.class);
        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(FIRST_CLASS_POST);
        when(noticeOfPossessionEntity.getNoticeDate()).thenReturn(postedDate);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setNoticeServedDetails(noticeServedDetailsCaptor.capture());

        NoticeServedDetails noticeServedDetails = noticeServedDetailsCaptor.getValue();
        assertThat(noticeServedDetails.getNoticeServiceMethod()).isEqualTo(FIRST_CLASS_POST);
        assertThat(noticeServedDetails.getNoticePostedDate()).isSameAs(postedDate);
    }

    @Test
    void shouldSetNoticeServedDateForDelivered() {
        // Given
        LocalDate deliveredDate = mock(LocalDate.class);
        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(DELIVERED_PERMITTED_PLACE);
        when(noticeOfPossessionEntity.getNoticeDate()).thenReturn(deliveredDate);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setNoticeServedDetails(noticeServedDetailsCaptor.capture());

        NoticeServedDetails noticeServedDetails = noticeServedDetailsCaptor.getValue();
        assertThat(noticeServedDetails.getNoticeServiceMethod()).isEqualTo(DELIVERED_PERMITTED_PLACE);
        assertThat(noticeServedDetails.getNoticeDeliveredDate()).isSameAs(deliveredDate);
    }

    @Test
    void shouldSetNoticeServedDateAndRecipientForPersonallyHanded() {
        // Given
        LocalDateTime handedOverDateTime = mock(LocalDateTime.class);
        String recipientName = "some recipient";

        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(PERSONALLY_HANDED);
        when(noticeOfPossessionEntity.getNoticeDateTime()).thenReturn(handedOverDateTime);
        when(noticeOfPossessionEntity.getNoticeDetails()).thenReturn(recipientName);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setNoticeServedDetails(noticeServedDetailsCaptor.capture());

        NoticeServedDetails noticeServedDetails = noticeServedDetailsCaptor.getValue();
        assertThat(noticeServedDetails.getNoticeServiceMethod()).isEqualTo(PERSONALLY_HANDED);
        assertThat(noticeServedDetails.getNoticeHandedOverDateTime()).isSameAs(handedOverDateTime);
        assertThat(noticeServedDetails.getNoticePersonName()).isEqualTo(recipientName);
    }

    @Test
    void shouldSetNoticeServedDateAndDetailsForEmail() {
        // Given
        LocalDateTime emailSentDateTime = mock(LocalDateTime.class);
        String emailExplanation = "some email details";

        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(EMAIL);
        when(noticeOfPossessionEntity.getNoticeDateTime()).thenReturn(emailSentDateTime);
        when(noticeOfPossessionEntity.getNoticeDetails()).thenReturn(emailExplanation);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setNoticeServedDetails(noticeServedDetailsCaptor.capture());

        NoticeServedDetails noticeServedDetails = noticeServedDetailsCaptor.getValue();
        assertThat(noticeServedDetails.getNoticeServiceMethod()).isEqualTo(EMAIL);
        assertThat(noticeServedDetails.getNoticeEmailSentDateTime()).isSameAs(emailSentDateTime);
        assertThat(noticeServedDetails.getNoticeEmailExplanation()).isEqualTo(emailExplanation);
    }

    @Test
    void shouldSetNoticeServedDateForOtherElectronic() {
        // Given
        LocalDateTime otherElectronicDateTime = mock(LocalDateTime.class);

        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(OTHER_ELECTRONIC);
        when(noticeOfPossessionEntity.getNoticeDateTime()).thenReturn(otherElectronicDateTime);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setNoticeServedDetails(noticeServedDetailsCaptor.capture());

        NoticeServedDetails noticeServedDetails = noticeServedDetailsCaptor.getValue();
        assertThat(noticeServedDetails.getNoticeServiceMethod()).isEqualTo(OTHER_ELECTRONIC);
        assertThat(noticeServedDetails.getNoticeOtherElectronicDateTime()).isSameAs(otherElectronicDateTime);
    }

    @Test
    void shouldSetNoticeServedDateAndDetailsForOther() {
        // Given
        LocalDateTime otherDateTime = mock(LocalDateTime.class);
        String otherExplanation = "some other details";

        when(noticeOfPossessionEntity.getServingMethod()).thenReturn(OTHER);
        when(noticeOfPossessionEntity.getNoticeDateTime()).thenReturn(otherDateTime);
        when(noticeOfPossessionEntity.getNoticeDetails()).thenReturn(otherExplanation);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setNoticeServedDetails(noticeServedDetailsCaptor.capture());

        NoticeServedDetails noticeServedDetails = noticeServedDetailsCaptor.getValue();
        assertThat(noticeServedDetails.getNoticeServiceMethod()).isEqualTo(OTHER);
        assertThat(noticeServedDetails.getNoticeOtherDateTime()).isSameAs(otherDateTime);
        assertThat(noticeServedDetails.getNoticeOtherExplanation()).isEqualTo(otherExplanation);
    }

}
