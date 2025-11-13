
package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.extern.slf4j.Slf4j;
import org.instancio.Instancio;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Service for generating fully populated test case instances using Instancio.
 */
@Service
@Slf4j
public class TestCaseService {

    public PCSCase generateTestPCSCase() {
        try {
            PCSCase pcsCase = Instancio.create(PCSCase.class);
            if (pcsCase.getPropertyAddress() == null) {
                pcsCase.setPropertyAddress(createDefaultAddress());
            }
            return pcsCase;

        } catch (Exception e) {
            log.error("Failed to generate test PCSCase", e);
            throw new RuntimeException("Failed to generate test PCSCase", e);
        }
    }

    private AddressUK createDefaultAddress() {
        return AddressUK.builder()
            .addressLine1("123 Test Street")
            .addressLine2("Test District")
            .postTown("London")
            .postCode("SW1A 1AA")
            .country("United Kingdom")
            .build();
    }
}
