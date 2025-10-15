package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EnforcementApplicationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.NameAndAddressForEvictionPage;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    @Mock
    private SavingPageBuilderFactory savingPageBuilderFactory;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private ModelMapper modelMapper;

    private SavingPageBuilder savingPageBuilder;
    private EnforcementOrderEvent underTest;

    private final ArgumentCaptor<CcdPageConfiguration> captor = ArgumentCaptor.forClass(CcdPageConfiguration.class);

    @BeforeEach
    void setUp() {
        setupSavingPageBuilder();

        underTest = new EnforcementOrderEvent(savingPageBuilderFactory, draftCaseDataService, modelMapper);

        setEventUnderTest(underTest);
    }

    @Test
    void shouldReturnCaseDataInStartCallback() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .addressLine2("Marylebone")
                                 .postTown("London")
                                 .build()).build();
        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE)).thenReturn(Optional.of(caseData));

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenNoUnsubmittedDataFound() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE))
            .thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> callStartHandler(caseData));

        // Then
        assertThat(throwable)
            .isNotNull()
            .hasMessageContaining("No unsubmitted case data found for case");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldConfigurePagesWithSavingPageBuilder() {
        // Given
        setupSavingPageBuilder();
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = mock(Event.EventBuilder.class);

        // When
        underTest.configurePages(eventBuilder);

        // Then
        verify(savingPageBuilderFactory).create(eventBuilder);
        verify(savingPageBuilder, times(2)).add(captor.capture());

        List<CcdPageConfiguration> capturedPages = captor.getAllValues();

        // Verify what was actually added
        assertThat(capturedPages).hasSize(2);
        assertThat(capturedPages.get(0)).isInstanceOf(EnforcementApplicationPage.class);
        assertThat(capturedPages.get(1)).isInstanceOf(NameAndAddressForEvictionPage.class);
    }

    @Test
    void shouldPreserveExistingCaseDataWhenMappingUnsubmittedData() {
        // Given
        PCSCase currentCaseData = PCSCase.builder().propertyAddress(AddressUK.builder()
                                 .addressLine1("Original Address")
                                 .build()).build();

        PCSCase unsubmittedData = PCSCase.builder().propertyAddress(AddressUK.builder()
                                 .addressLine1("Updated Address")
                                 .build()).build();

        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE))
            .thenReturn(Optional.of(unsubmittedData));

        // When
        PCSCase result = callStartHandler(currentCaseData);

        // Then
        assertThat(result).isSameAs(currentCaseData);
        verify(modelMapper).map(unsubmittedData, currentCaseData);
    }

    private void setupSavingPageBuilder() {
        savingPageBuilder = mock(SavingPageBuilder.class);
        when(savingPageBuilderFactory.create(any())).thenReturn(savingPageBuilder);
        when(savingPageBuilder.add(any())).thenReturn(savingPageBuilder);
    }

}
