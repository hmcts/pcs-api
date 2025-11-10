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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcement.EnforcementDataService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    private final AddressFormatter addressFormatter = new AddressFormatter();

    @Mock
    private EnforcementDataService enforcementDataService;

    @Mock
    private PcsCaseService pcsCaseService;

    private static final long CASE_REFERENCE = 1234L;

    @BeforeEach
    void setUp() {
        EnforcementOrderEvent enforcementOrderEvent =
                new EnforcementOrderEvent(enforcementDataService, pcsCaseService, addressFormatter);
        setEventUnderTest(enforcementOrderEvent);
    }

    @Test
    void shouldReturnCaseDataInStartCallback() {
        // Given
        String firstName = "Test";
        String lastName = "Testing";
        DefendantDetails defendantDetails = DefendantDetails.builder().firstName(firstName).lastName(lastName).build();
        PCSCase caseData = PCSCase.builder()
                .defendants(List.of(ListValue.<DefendantDetails>builder().value(defendantDetails).build()))
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .addressLine2("Marylebone")
                                 .postTown("London")
                                 .build()).build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFormattedPropertyAddress()).isEqualTo(addressFormatter.getFormattedAddress(caseData));
        assertThat(result.getDefendants()).hasSize(1);
        assertThat(result.getDefendant1().getFirstName()).isEqualTo(firstName);
        assertThat(result.getDefendant1().getLastName()).isEqualTo(lastName);
    }

    @Test
    void shouldCreateSubmitteedEnforcementDataInSubmitCallback() {
        // Given

        EnforcementDataEntity enforcementDataEntity = mock(EnforcementDataEntity.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        PCSCase pcsCase = mock(PCSCase.class);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(enforcementDataService.createEnforcementData(CASE_REFERENCE, pcsCase)).thenReturn(enforcementDataEntity);

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseEntity).setEnforcementDataEntities(Set.of(enforcementDataEntity));
        verify(pcsCaseService).save(pcsCaseEntity);
    }
}
