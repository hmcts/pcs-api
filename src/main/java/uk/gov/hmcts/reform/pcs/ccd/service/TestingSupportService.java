
package uk.gov.hmcts.reform.pcs.ccd.service;

import org.instancio.Instancio;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;

/**
 * Service for generating fully populated test case instances using Instancio.
 * TVR: Profile this to be under dev and preview environments only !!!
 */
@Service
public class TestingSupportService {

    public PCSCase generateTestPCSCase(PCSCase fromEvent) {
        try {



            return fromEvent;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test PCSCase", e);
        }
    }

}
