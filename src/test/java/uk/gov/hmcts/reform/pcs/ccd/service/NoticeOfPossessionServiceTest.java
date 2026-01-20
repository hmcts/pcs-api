package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeOfPossessionServiceTest {

    @Mock
    private PCSCase pcsCase;
    @Mock(strictness = LENIENT)
    private NoticeServedDetails noticeServedDetails;
    @Mock
    private WalesNoticeDetails walesNoticeDetails;

    private NoticeOfPossessionService underTest;

    @BeforeEach
    void setUp() {
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(NoticeServiceMethod.EMAIL);

        underTest = new NoticeOfPossessionService();
    }

    @Test
    void shouldReturnNullIfNoticeServedDetailsIsNull() {
        // Given
        when(pcsCase.getNoticeServedDetails()).thenReturn(null);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity).isNull();
    }

    @Test
    void shouldReturnNullIfNoticeServedMethodIsNull() {
        // Given
        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(null);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = YesOrNo.class)
    void shouldSetNoticeServedAndTypeForWales(YesOrNo noticeServed) {
        // Given
        String typeOfNotice = "type of notice";

        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.WALES);
        when(pcsCase.getWalesNoticeDetails()).thenReturn(walesNoticeDetails);
        when(walesNoticeDetails.getNoticeServed()).thenReturn(noticeServed);
        when(walesNoticeDetails.getTypeOfNoticeServed()).thenReturn(typeOfNotice);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeServed()).isEqualTo(noticeServed);
        assertThat(noticeOfPossessionEntity.getNoticeType()).isEqualTo(typeOfNotice);
    }

    @ParameterizedTest
    @EnumSource(value = YesOrNo.class)
    void shouldSetNoticeServedForNonWales(YesOrNo noticeServed) {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.ENGLAND);
        when(pcsCase.getNoticeServed()).thenReturn(noticeServed);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeServed()).isEqualTo(noticeServed);
    }

    @ParameterizedTest
    @EnumSource(value = NoticeServiceMethod.class)
    void shouldSetNoticeServedMethod(NoticeServiceMethod noticeServedMethod) {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.ENGLAND);
        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(noticeServedMethod);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getServingMethod()).isEqualTo(noticeServedMethod);
    }

    @Test
    void shouldSetNoticeServedDateForPost() {
        // Given
        LocalDate postedDate = mock(LocalDate.class);
        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(NoticeServiceMethod.FIRST_CLASS_POST);
        when(noticeServedDetails.getNoticePostedDate()).thenReturn(postedDate);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDate()).isSameAs(postedDate);
    }

    @Test
    void shouldSetNoticeServedDateForDelivered() {
        // Given
        LocalDate deliveredDate = mock(LocalDate.class);
        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE);
        when(noticeServedDetails.getNoticeDeliveredDate()).thenReturn(deliveredDate);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDate()).isSameAs(deliveredDate);
    }

    @Test
    void shouldSetNoticeServedDateAndRecipientForPersonallyHanded() {
        // Given
        LocalDateTime handedOverDateTime = mock(LocalDateTime.class);
        String recipientName = "some recipient";

        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(NoticeServiceMethod.PERSONALLY_HANDED);
        when(noticeServedDetails.getNoticeHandedOverDateTime()).thenReturn(handedOverDateTime);
        when(noticeServedDetails.getNoticePersonName()).thenReturn(recipientName);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDateTime()).isSameAs(handedOverDateTime);
        assertThat(noticeOfPossessionEntity.getNoticeDetails()).isEqualTo(recipientName);
    }

    @Test
    void shouldSetNoticeServedDateAndDetailsForEmail() {
        // Given
        LocalDateTime emailSentDateTime = mock(LocalDateTime.class);
        String emailExplanation = "some email details";

        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(NoticeServiceMethod.EMAIL);
        when(noticeServedDetails.getNoticeEmailSentDateTime()).thenReturn(emailSentDateTime);
        when(noticeServedDetails.getNoticeEmailExplanation()).thenReturn(emailExplanation);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDateTime()).isSameAs(emailSentDateTime);
        assertThat(noticeOfPossessionEntity.getNoticeDetails()).isEqualTo(emailExplanation);
    }

    @Test
    void shouldSetNoticeServedDateForOtherElectronic() {
        // Given
        LocalDateTime otherElectronicDateTime = mock(LocalDateTime.class);

        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(NoticeServiceMethod.OTHER_ELECTRONIC);
        when(noticeServedDetails.getNoticeOtherElectronicDateTime()).thenReturn(otherElectronicDateTime);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDateTime()).isSameAs(otherElectronicDateTime);
    }

    @Test
    void shouldSetNoticeServedDateAndDetailsForOther() {
        // Given
        LocalDateTime otherDateTime = mock(LocalDateTime.class);
        String otherExplanation = "some other details";

        when(noticeServedDetails.getNoticeServiceMethod()).thenReturn(NoticeServiceMethod.OTHER);
        when(noticeServedDetails.getNoticeOtherDateTime()).thenReturn(otherDateTime);
        when(noticeServedDetails.getNoticeOtherExplanation()).thenReturn(otherExplanation);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDateTime()).isSameAs(otherDateTime);
        assertThat(noticeOfPossessionEntity.getNoticeDetails()).isEqualTo(otherExplanation);
    }

}
