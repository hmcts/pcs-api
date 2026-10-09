package uk.gov.hmcts.reform.pcs.ccd.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

    /**
     * The order the frontend names in the Client-Context header, since CCD passes no parameters to the
     * start of an event; anything named that is not an order is refused.
     */
    public Optional<UUID> getOrderId() {
        ClientContext clientContext = getClientContext();
        try {
            return Optional.ofNullable(clientContext == null ? null : clientContext.getOrderId()).map(UUID::fromString);
        } catch (IllegalArgumentException e) {
            throw ExternalRejection.because("The link to the order is not valid");
        }
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes)
            Objects.requireNonNull(requestAttributesSupplier.get())
        ).getRequest();
    }

}
