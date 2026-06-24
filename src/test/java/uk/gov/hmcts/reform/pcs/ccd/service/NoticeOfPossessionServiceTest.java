package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeOfPossessionServiceTest {

    @Mock(strictness = LENIENT)
    private PCSCase pcsCase;
    @Mock(strictness = LENIENT)
    private NoticeServedDetails noticeServedDetails;
    @Mock(strictness = LENIENT)
    private WalesNoticeDetails walesNoticeDetails;

    private NoticeOfPossessionService underTest;

    @BeforeEach
    void setUp() {
        when(pcsCase.getNoticeServedDetails()).thenReturn(noticeServedDetails);
        when(noticeServedDetails.getServiceMethod()).thenReturn(NoticeServiceMethod.EMAIL);

        underTest = new NoticeOfPossessionService();
    }

    @Test
    void shouldNotSetNoticeDetailsIfNoticeNotServed() {
        // Given
        when(pcsCase.getNoticeServed()).thenReturn(YesOrNo.NO);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        verifyNoInteractions(noticeServedDetails);

        assertThat(noticeOfPossessionEntity)
            .usingRecursiveComparison()
            .isEqualTo(NoticeOfPossessionEntity.builder()
                           .noticeServed(YesOrNo.NO)
                           .build()
            );
    }

    @ParameterizedTest
    @EnumSource(value = YesOrNo.class)
    void shouldSetNoticeServedForWales(YesOrNo noticeServed) {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.WALES);
        when(pcsCase.getWalesNoticeDetails()).thenReturn(walesNoticeDetails);
        when(walesNoticeDetails.getNoticeServed()).thenReturn(noticeServed);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeServed()).isEqualTo(noticeServed);
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

    @Test
    void shouldSetNoticeTypeForWalesWhenNoticeServed() {
        // Given
        String typeOfNotice = "type of notice";
        String noticeStatement = "notice statement";

        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.WALES);
        when(pcsCase.getWalesNoticeDetails()).thenReturn(walesNoticeDetails);
        when(walesNoticeDetails.getNoticeServed()).thenReturn(YesOrNo.YES);
        when(walesNoticeDetails.getTypeOfNoticeServed()).thenReturn(typeOfNotice);
        when(walesNoticeDetails.getNoticeStatement()).thenReturn(noticeStatement);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeType()).isEqualTo(typeOfNotice);
        assertThat(noticeOfPossessionEntity.getNoticeStatement()).isEqualTo(noticeStatement);
    }

    @Test
    void shouldSetNoticeStatementForWalesWhenNoticeNotServed() {
        // Given
        String noticeStatement = "notice statement";

        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.WALES);
        when(pcsCase.getWalesNoticeDetails()).thenReturn(walesNoticeDetails);
        when(walesNoticeDetails.getNoticeServed()).thenReturn(YesOrNo.NO);
        when(walesNoticeDetails.getNoticeStatement()).thenReturn(noticeStatement);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeType()).isNull();
        assertThat(noticeOfPossessionEntity.getNoticeStatement()).isEqualTo(noticeStatement);
    }

    @ParameterizedTest
    @EnumSource(value = NoticeServiceMethod.class)
    void shouldSetNoticeServedMethod(NoticeServiceMethod noticeServedMethod) {
        // Given
        when(pcsCase.getLegislativeCountry()).thenReturn(LegislativeCountry.ENGLAND);
        when(noticeServedDetails.getServiceMethod()).thenReturn(noticeServedMethod);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getServingMethod()).isEqualTo(noticeServedMethod);
    }

    @Test
    void shouldSetNoticeServedDateForPost() {
        // Given
        LocalDate postedDate = mock(LocalDate.class);
        when(noticeServedDetails.getServiceMethod()).thenReturn(NoticeServiceMethod.FIRST_CLASS_POST);
        when(noticeServedDetails.getPostedDate()).thenReturn(postedDate);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDate()).isSameAs(postedDate);
    }

    @Test
    void shouldSetNoticeServedDateForDelivered() {
        // Given
        LocalDate deliveredDate = mock(LocalDate.class);
        when(noticeServedDetails.getServiceMethod()).thenReturn(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE);
        when(noticeServedDetails.getDeliveredDate()).thenReturn(deliveredDate);

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

        when(noticeServedDetails.getServiceMethod()).thenReturn(NoticeServiceMethod.PERSONALLY_HANDED);
        when(noticeServedDetails.getHandedOverDateTime()).thenReturn(handedOverDateTime);
        when(noticeServedDetails.getPersonName()).thenReturn(recipientName);

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
        String emailAddress = "name@example.com";

        when(noticeServedDetails.getServiceMethod()).thenReturn(NoticeServiceMethod.EMAIL);
        when(noticeServedDetails.getEmailSentDateTime()).thenReturn(emailSentDateTime);
        when(noticeServedDetails.getEmailAddress()).thenReturn(emailAddress);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDateTime()).isSameAs(emailSentDateTime);
        assertThat(noticeOfPossessionEntity.getNoticeDetails()).isEqualTo(emailAddress);
    }

    @Test
    void shouldSetNoticeServedDateAndDetailsForOtherElectronic() {
        // Given
        LocalDateTime otherElectronicDateTime = mock(LocalDateTime.class);
        String details = "details";

        when(noticeServedDetails.getServiceMethod()).thenReturn(NoticeServiceMethod.OTHER_ELECTRONIC);
        when(noticeServedDetails.getOtherElectronicDateTime()).thenReturn(otherElectronicDateTime);
        when(noticeServedDetails.getOtherElectronicExplanation()).thenReturn(details);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDateTime()).isSameAs(otherElectronicDateTime);
        assertThat(noticeOfPossessionEntity.getNoticeDetails()).isEqualTo(details);
    }

    @Test
    void shouldSetNoticeServedDateAndDetailsForOther() {
        // Given
        LocalDateTime otherDateTime = mock(LocalDateTime.class);
        String otherExplanation = "some other details";

        when(noticeServedDetails.getServiceMethod()).thenReturn(NoticeServiceMethod.OTHER);
        when(noticeServedDetails.getOtherDateTime()).thenReturn(otherDateTime);
        when(noticeServedDetails.getOtherExplanation()).thenReturn(otherExplanation);

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(pcsCase);

        // Then
        assertThat(noticeOfPossessionEntity.getNoticeDateTime()).isSameAs(otherDateTime);
        assertThat(noticeOfPossessionEntity.getNoticeDetails()).isEqualTo(otherExplanation);
    }

    @Test
    void shouldSetUnableToUploadReasonIfAbleToUploadDocumentIsFalse() {
        // Given
        String unableToUploadReason = "reason for unable to upload";
        NoticeServedDetails details = NoticeServedDetails.builder()
                        .serviceMethod(NoticeServiceMethod.EMAIL)
                        .documents(List.of())
                        .ableToUploadDocument(CanUploadNoticeServedDocument.No)
                        .unableToUploadReason(unableToUploadReason)
                        .build();
        PCSCase caseData = PCSCase.builder()
                        .noticeServed(YesOrNo.YES)
                        .legislativeCountry(LegislativeCountry.ENGLAND)
                        .noticeServedDetails(details)
                        .build();

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(caseData);

        // Then
        assertThat(noticeOfPossessionEntity.getIsAbleToUploadDocument()).isEqualTo(YesOrNo.NO);
        assertThat(noticeOfPossessionEntity.getUnableToUploadReason()).isEqualTo(unableToUploadReason);
    }

    @Test
    void shouldNotSetUnableToUploadReasonIfAbleToUploadDocumentIsTrue() {
        // Given
        String unableToUploadReason = "reason for unable to upload";
        NoticeServedDetails details = NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.EMAIL)
                .documents(List.of())
                .ableToUploadDocument(CanUploadNoticeServedDocument.Yes)
                .unableToUploadReason(unableToUploadReason)
                .build();
        PCSCase caseData = PCSCase.builder()
                .noticeServed(YesOrNo.YES)
                .legislativeCountry(LegislativeCountry.ENGLAND)
                .noticeServedDetails(details)
                .build();

        // When
        NoticeOfPossessionEntity noticeOfPossessionEntity = underTest.createNoticeOfPossessionEntity(caseData);

        // Then
        assertThat(noticeOfPossessionEntity.getIsAbleToUploadDocument()).isEqualTo(YesOrNo.YES);
        assertThat(noticeOfPossessionEntity.getUnableToUploadReason()).isNull();
    }
}
