package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcement.EnforcementDataService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    @Mock
    private final AddressFormatter addressFormatter = new AddressFormatter();

    @Mock
    private EnforcementDataService enforcementDataService;

    private static final long CASE_REFERENCE = 1234L;

    @BeforeEach
    void setUp() {
        EnforcementOrderEvent enforcementOrderEvent =
                new EnforcementOrderEvent(enforcementDataService, addressFormatter);
        setEventUnderTest(enforcementOrderEvent);
    }

    @Test
    void shouldReturnCaseDataInStartCallback() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        String expectedFormattedPropertyAddress = "expected formatted property address";

        String firstName = "Test";
        String lastName = "Testing";
        DefendantDetails defendantDetails = DefendantDetails.builder().firstName(firstName).lastName(lastName).build();
        PCSCase caseData = PCSCase.builder()
                .defendants(List.of(ListValue.<DefendantDetails>builder().value(defendantDetails).build()))
            .propertyAddress(propertyAddress).build();

        when(addressFormatter.formatAddressWithHtmlLineBreaks(propertyAddress))
            .thenReturn(expectedFormattedPropertyAddress);

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFormattedPropertyAddress()).isEqualTo(expectedFormattedPropertyAddress);
        assertThat(result.getDefendants()).hasSize(1);
        assertThat(result.getDefendant1().getFirstName()).isEqualTo(firstName);
        assertThat(result.getDefendant1().getLastName()).isEqualTo(lastName);
    }

    @Test
    void shouldCreateSubmittedEnforcementDataInSubmitCallback() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        EnforcementDataEntity enforcementDataEntity = new EnforcementDataEntity();
        enforcementDataEntity.setId(UUID.randomUUID());

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(enforcementDataService).createEnforcementData(CASE_REFERENCE, enforcementOrder);
    }
}
