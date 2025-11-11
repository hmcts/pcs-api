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
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AdditionalInformationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AggressiveAnimalsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.CriminalAntisocialRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.FirearmsPossessionRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.PoliceOrSocialServicesRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.PropertyAccessDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.ProtestorGroupRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.VerbalOrWrittenThreatsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.ViolentAggressiveRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.VulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    private final AddressFormatter addressFormatter = new AddressFormatter();
    @Mock
    private ViolentAggressiveRiskPage violentAggressiveRiskPage;
    @Mock
    private VerbalOrWrittenThreatsRiskPage verbalOrWrittenThreatsRiskPage;
    @Mock
    private ProtestorGroupRiskPage protestorGroupRiskPage;
    @Mock
    private PoliceOrSocialServicesRiskPage policeOrSocialServicesRiskPage;
    @Mock
    private FirearmsPossessionRiskPage firearmsPossessionRiskPage;
    @Mock
    private CriminalAntisocialRiskPage criminalAntisocialRiskPage;
    @Mock
    private AggressiveAnimalsRiskPage aggressiveAnimalsRiskPage;
    @Mock
    private PropertyAccessDetailsPage propertyAccessDetailsPage;
    @Mock
    private VulnerableAdultsChildrenPage vulnerableAdultsChildrenPage;
    @Mock
    private AdditionalInformationPage additionalInformationPage;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new EnforcementOrderEvent(addressFormatter, violentAggressiveRiskPage,
                                                    verbalOrWrittenThreatsRiskPage, protestorGroupRiskPage,
                                                    policeOrSocialServicesRiskPage, firearmsPossessionRiskPage,
                                                    criminalAntisocialRiskPage, aggressiveAnimalsRiskPage,
                                                    propertyAccessDetailsPage, vulnerableAdultsChildrenPage,
                                                    additionalInformationPage));
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

}
