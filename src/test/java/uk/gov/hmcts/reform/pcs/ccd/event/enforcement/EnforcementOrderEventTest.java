package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    @Mock
    private final AddressFormatter addressFormatter = new AddressFormatter();
    @Mock
    private SavingPageBuilderFactory savingPageBuilderFactory;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        SavingPageBuilder savingPageBuilder = mock(SavingPageBuilder.class);
        when(savingPageBuilder.add(any())).thenReturn(savingPageBuilder);
        when(savingPageBuilderFactory.create(any(Event.EventBuilder.class), eq(enforceTheOrder)))
            .thenReturn(savingPageBuilder);
        setEventUnderTest(new EnforcementOrderEvent(addressFormatter, savingPageBuilderFactory));
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

}
