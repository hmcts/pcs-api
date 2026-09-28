package uk.gov.hmcts.reform.pcs.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.ccd.sdk.testing.EnableCcdEventTesting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs CCD events against the whole pcs-api application. Put it on a test that extends
 * {@link AbstractPostgresContainerIT}, for the database. The SDK test support answers IDAM, S2S and
 * role assignment, so a test registers actors rather than stubbing clients.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("integration")
@EnableCcdEventTesting
@Import(IssuedCases.class)
public @interface PcsCcdEventTest {
}
