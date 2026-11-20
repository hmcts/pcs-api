package uk.gov.hmcts.reform.pcs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.docassembly.DocAssemblyClient;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;

/**
 * Kept off Application so test slices such as @DataJpaTest, which skip scanned configuration
 * and Feign, don't try to create these clients.
 */
@Configuration(proxyBeanMethods = false)
@Import({PaymentsClient.class, DocAssemblyClient.class})
public class HmctsClientConfiguration {
}
