package uk.gov.hmcts.reform.pcs.ccd.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
import java.util.function.Supplier;

@Component
@RequestScope
public class ClientContextRetriever {

    private static final String CLIENT_CONTEXT_HEADER_KEY = "Client-context";
    private final Supplier<ObjectMapper> objectMapperSupplier;
    private final Supplier<RequestAttributes> requestAttributesSupplier;

    public ClientContextRetriever(Supplier<ObjectMapper> objectMapperSupplier,
                                  Supplier<RequestAttributes> requestAttributesSupplier) {
        this.objectMapperSupplier = objectMapperSupplier;
        this.requestAttributesSupplier = requestAttributesSupplier;
    }

    @Autowired
    public ClientContextRetriever() {
        this(ObjectMapper::new, RequestContextHolder::getRequestAttributes);
    }

    public ClientContext getClientContext() {
        String clientContextAsStringJson = getRequest().getHeader(CLIENT_CONTEXT_HEADER_KEY);
        if (clientContextAsStringJson == null) {
            return null;
        }
        try {
            ObjectMapper objectMapper = objectMapperSupplier.get();
            return objectMapper.readValue(clientContextAsStringJson, ClientContext.class);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse Client-context",e);
        }
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes)
            Objects.requireNonNull(requestAttributesSupplier.get())
        ).getRequest();
    }

}
