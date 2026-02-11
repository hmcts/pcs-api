package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.EventTypeBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder.EnforceTheOrder;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestCaseSupportHelper;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestCaseSupportException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;
import static uk.gov.hmcts.reform.pcs.ccd.event.TestCaseGeneration.EVENT_NAME;
import static uk.gov.hmcts.reform.pcs.ccd.event.TestCaseGeneration.NO_NON_PROD_CASE_AVAILABLE;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;

@ExtendWith(MockitoExtension.class)
class TestCaseGenerationTest {

    @InjectMocks
    private TestCaseGeneration underTest;

    @Mock
    private TestCaseSupportHelper testCaseSupportHelper;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ResumePossessionClaim resumePossessionClaim;
    @Mock
    private EnforceTheOrder enforceTheOrder;

    @Test
    @SuppressWarnings("unchecked")
    void shouldSuccessfullyConfigureDecentralisedEvent() {
        // Given
        DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder = mock(DecentralisedConfigBuilder.class);
        EventTypeBuilder<PCSCase, UserRole, State> typeBuilder = mock(EventTypeBuilder.class);
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = mock(Event.EventBuilder.class, RETURNS_SELF);
        FieldCollection.FieldCollectionBuilder<PCSCase, State, Event.EventBuilder<PCSCase, UserRole, State>> f =
            mock(FieldCollection.FieldCollectionBuilder.class, RETURNS_SELF);
        doReturn(typeBuilder).when(configBuilder).decentralisedEvent(anyString(), any(), any());
        when(typeBuilder.initialState(any())).thenReturn(eventBuilder);
        when(eventBuilder.fields()).thenReturn(f);

        // When
        underTest.configure(configBuilder);

        // Then
        verify(configBuilder).decentralisedEvent(eq(createTestCase.name()), any(), any());
        verify(typeBuilder).initialState(AWAITING_SUBMISSION_TO_HMCTS);
        verify(eventBuilder).showSummary();
        verify(eventBuilder).name(EVENT_NAME);
        verify(eventBuilder).grant(Permission.CRUD, UserRole.PCS_SOLICITOR);
    }

    @Test
    void shouldReturnDynamicListWhenTestCaseSupportFileListIsPresent() {
        // Given
        DynamicList expectedList = DynamicList.builder().build();
        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCase.getTestCaseSupportFileList()).thenReturn(expectedList);

        // When
        DynamicList actualList = underTest.getTestFilesList(pcsCase);

        // Then
        assertThat(actualList).isEqualTo(expectedList);
    }

    @Test
    void shouldThrowExceptionWhenTestCaseSupportFileListIsNull() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        when(pcsCase.getTestCaseSupportFileList()).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> underTest.getTestFilesList(pcsCase))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(NO_NON_PROD_CASE_AVAILABLE);
    }

    @Test
    void shouldLoadTestPcsCaseSuccessfully() throws IOException {
        // Given
        String label = "test-label";
        String jsonContent = "{\"propertyAddress\": {}}";
        Resource resource = mock(Resource.class);
        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
        PCSCase expectedCase = PCSCase.builder().build();

        when(testCaseSupportHelper.getTestResource(label)).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(draftCaseDataService.parseCaseDataJson(jsonContent)).thenReturn(expectedCase);

        // When
        PCSCase actualCase = underTest.loadTestPcsCase(label);

        // Then
        assertThat(actualCase).isEqualTo(expectedCase);
        verify(testCaseSupportHelper).getTestResource(label);
        verify(draftCaseDataService).parseCaseDataJson(jsonContent);
    }

    @Test
    void shouldThrowTestCaseSupportExceptionWhenIOExceptionOccurs() throws IOException {
        // Given
        String label = "test-label";
        IOException ioException = new IOException("File not found");
        when(testCaseSupportHelper.getTestResource(label)).thenThrow(ioException);

        // When / Then
        assertThatThrownBy(() -> underTest.loadTestPcsCase(label))
            .isInstanceOf(TestCaseSupportException.class)
            .hasCause(ioException);
    }

    @Test
    void shouldSuccessfullyMakeAClaimTestCreation() {
        // Given
        String label = "test-label";
        long caseReference = 123456L;
        AddressUK address = AddressUK.builder().addressLine1("102").postCode("SW1 1AA").build();
        LegislativeCountry country = ENGLAND;

        PCSCase loadedCase = PCSCase.builder()
            .propertyAddress(address)
            .legislativeCountry(country)
            .build();

        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        TestCaseGeneration spyUnderTest = spy(underTest);
        doReturn(loadedCase).when(spyUnderTest).loadTestPcsCase(label);

        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // When
        spyUnderTest.makeAClaimTestCreation(label, caseReference);

        // Then
        InOrder inOrder = inOrder(pcsCaseService, resumePossessionClaim);
        inOrder.verify(pcsCaseService).createCase(caseReference, address, country);
        inOrder.verify(resumePossessionClaim).submitClaim(caseReference, loadedCase);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSubmitMakeAClaimBasicCase() {
        // Given
        Long caseReference = 123L;
        String label = TestCaseGeneration.MAKE_A_CLAIM_CASE_GENERATOR;

        DynamicList testFilesList = DynamicList.builder()
            .value(DynamicListElement.builder().label(label).build())
            .build();

        PCSCase pcsCase = PCSCase.builder().testCaseSupportFileList(testFilesList).build();

        EventPayload<PCSCase, State> eventPayload = mock(EventPayload.class);
        when(eventPayload.caseReference()).thenReturn(caseReference);
        when(eventPayload.caseData()).thenReturn(pcsCase);

        TestCaseGeneration spyUnderTest = spy(underTest);
        doNothing().when(spyUnderTest).makeAClaimTestCreation(anyString(), anyLong());

        // When
        SubmitResponse<State> response = spyUnderTest.submit(eventPayload);

        // Then
        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
        verify(spyUnderTest).makeAClaimTestCreation(label, caseReference);
    }

    @Test
    void shouldSubmitEnforcementWarrantBasicCase() {
        // Given
        Long caseReference = 456L;
        String label = TestCaseGeneration.ENFORCEMENT_CASE_GENERATOR;

        DynamicList testFilesList = DynamicList.builder()
            .value(DynamicListElement.builder().label(label).build())
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .testCaseSupportFileList(testFilesList)
            .build();

        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, pcsCase, null);

        PCSCase loadedCase = PCSCase.builder().build();
        TestCaseGeneration spyUnderTest = spy(underTest);

        doNothing().when(spyUnderTest).makeAClaimTestCreation(anyString(), anyLong());
        doReturn(loadedCase).when(spyUnderTest).loadTestPcsCase(label);

        // When
        SubmitResponse<State> response = spyUnderTest.submit(eventPayload);

        // Then
        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
        verify(spyUnderTest).makeAClaimTestCreation(TestCaseGeneration.MAKE_A_CLAIM_CASE_GENERATOR, caseReference);
        verify(enforceTheOrder).submitOrder(caseReference, loadedCase);
    }

}
