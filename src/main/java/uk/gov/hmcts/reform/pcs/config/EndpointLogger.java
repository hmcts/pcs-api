package uk.gov.hmcts.reform.pcs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

@Slf4j
@Component
public class EndpointLogger implements ApplicationListener<ApplicationReadyEvent> {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public EndpointLogger(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("==============================================");
        log.info("===== REGISTERED ENDPOINTS =====");
        log.info("==============================================");

        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        map.forEach((key, value) -> {
            log.info("Endpoint: {} -> {}.{}",
                     key,
                     value.getBeanType().getSimpleName(),
                     value.getMethod().getName()
            );
        });

        log.info("==============================================");
        log.info("Total endpoints registered: {}", map.size());
        log.info("==============================================");
    }
}
