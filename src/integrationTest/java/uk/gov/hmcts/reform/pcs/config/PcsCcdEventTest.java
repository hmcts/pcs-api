package uk.gov.hmcts.reform.pcs.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.ccd.sdk.testing.EnableCcdEventTesting;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs CCD events against the whole pcs-api application. Put it on a test that extends
 * {@link AbstractPostgresContainerIT}, for the database. The SDK test support answers IDAM, S2S and
 * role assignment, so a test registers actors rather than stubbing clients.
 *
 * <p>It does not answer CCD's case-users API, which the case view asks for the viewer's case roles to decide
 * what of enforcement they see and which enforcement events they are offered (DefendantEnforcementView). So
 * {@link CaseAssignmentApi} is a mock, which gives every viewer no case roles; a test can autowire it to stub
 * some.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// LaunchDarkly reads the flags these tests need from a file rather than the service.
@SpringBootTest(properties = {
    "launchdarkly.offline-mode=true",
    "launchdarkly.files=src/integrationTest/resources/launchdarkly/pcs-ccd-event-test-flags.json"
})
@ActiveProfiles("integration")
@EnableCcdEventTesting
@Import(IssuedCases.class)
@MockitoBean(types = CaseAssignmentApi.class)
public @interface PcsCcdEventTest {
}
