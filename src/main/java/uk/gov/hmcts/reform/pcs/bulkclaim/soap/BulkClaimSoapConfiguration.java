package uk.gov.hmcts.reform.pcs.bulkclaim.soap;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.server.EndpointExceptionResolver;
import org.springframework.ws.server.EndpointInvocationChain;
import org.springframework.ws.server.EndpointMapping;
import org.springframework.ws.server.endpoint.MessageEndpoint;
import org.springframework.ws.server.endpoint.adapter.MessageEndpointAdapter;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.soap.server.endpoint.SoapFaultAnnotationExceptionResolver;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Locale;

/**
 * The bulk claim SOAP service. The {@code MessageDispatcherServlet} sits under {@code /v2/bulk-claims} so the APIM
 * S2S filter's lock covers it.
 */
@Configuration
@EnableWs
public class BulkClaimSoapConfiguration {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
        ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        ServletRegistrationBean<MessageDispatcherServlet> registration =
            new ServletRegistrationBean<>(servlet, BulkClaimSoapFilter.SERVICES_PATH + "/*");
        registration.setName("messageDispatcherServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    /** SOAP 1.1 only, as PCOL; the bean name is the one the servlet looks up. */
    @Bean(name = "messageFactory")
    public SaajSoapMessageFactory messageFactory() {
        SaajSoapMessageFactory factory = new SaajSoapMessageFactory();
        factory.setSoapVersion(SoapVersion.SOAP_11);
        return factory;
    }

    /** Last resort for a payload no operation claims, so it is a fault rather than a bare HTTP error. */
    @Bean
    public EndpointMapping bulkClaimUnknownOperationMapping() {
        return messageContext -> new EndpointInvocationChain((MessageEndpoint) context -> {
            throw new BulkClaimSoapRefusalException("Unknown operation");
        });
    }

    @Bean
    public MessageEndpointAdapter bulkClaimMessageEndpointAdapter() {
        return new MessageEndpointAdapter();
    }

    /**
     * Exceptions marked {@code @SoapFault} become the fault they declare.
     */
    @Bean
    public EndpointExceptionResolver bulkClaimFaultResolver() {
        SoapFaultAnnotationExceptionResolver resolver = new SoapFaultAnnotationExceptionResolver();
        resolver.setOrder(0);
        return resolver;
    }

    /** Anything else is a Server fault that does not disclose the cause. */
    @Bean
    public EndpointExceptionResolver bulkClaimServerFaultResolver() {
        SoapFaultDefinition fault = new SoapFaultDefinition();
        fault.setFaultCode(SoapFaultDefinition.SERVER);
        fault.setFaultStringOrReason("The service was unable to process the request");
        fault.setLocale(Locale.ENGLISH);
        SoapFaultMappingExceptionResolver resolver = new SoapFaultMappingExceptionResolver();
        resolver.setDefaultFault(fault);
        resolver.setOrder(1);
        return resolver;
    }

    @Bean
    public FilterRegistrationBean<BulkClaimSoapFilter> bulkClaimSoapFilter(
        FeatureToggleService featureToggleService, BulkClaimProperties properties) {
        FilterRegistrationBean<BulkClaimSoapFilter> registration =
            new FilterRegistrationBean<>(new BulkClaimSoapFilter(featureToggleService, properties));
        registration.addUrlPatterns(BulkClaimSoapFilter.SERVICES_PATH + "/*");
        return registration;
    }
}
