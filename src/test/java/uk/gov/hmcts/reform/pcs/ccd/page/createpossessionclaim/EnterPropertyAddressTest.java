package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.EnterPropertyAddress;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class EnterPropertyAddressTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new EnterPropertyAddress());
    }

    @Test
    void shouldSetFormattedContactAddressInMidEventCallback() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("123 Baker Street")
            .addressLine2("Marylebone")
            .postTown("London")
            .county("Greater London")
            .postCode("NW1 6XE")
            .build();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        caseDetails.setData(caseData);

        String formattedAddress = formattedContactAddress(propertyAddress);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "enterPropertyAddress");
        midEvent.handle(caseDetails, null);

        // Then
        assertThat(caseData.getFormattedClaimantContactAddress()).isEqualTo(formattedAddress);
    }

    private String formattedContactAddress(AddressUK propertyAddress) {
        return  String.format(
            "%s<br>%s<br>%s",
            propertyAddress.getAddressLine1(),
            propertyAddress.getPostTown(),
            propertyAddress.getPostCode()
        );
    }

}
