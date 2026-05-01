package uk.gov.hmcts.reform.pcs.ccd.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientContextRetrieverTest {

    private ClientContextRetriever clientContextRetriever;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Supplier<ObjectMapper> objectMapperSupplier;

    @Mock
    private Supplier<RequestAttributes> requestAttributesSupplier;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @Mock
    private ClientContext clientContext;

    @BeforeEach
    void setUp() {
        clientContextRetriever = new ClientContextRetriever(objectMapperSupplier, requestAttributesSupplier);
    }

    @Test
    void getClientContext_WithoutHeader_ReturnsNull() {
        // given
        when(requestAttributesSupplier.get()).thenReturn(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(request);

        // when
        ClientContext actual = clientContextRetriever.getClientContext();

        // then
        assertNull(actual);
    }

    @Test
    void getClientContext_WithValidHeader_ReturnsClientContext() throws JsonProcessingException {
        // given
        String header = "abc";
        when(requestAttributesSupplier.get()).thenReturn(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(request);
        when(request.getHeader("Client-context")).thenReturn(header);
        when(objectMapperSupplier.get()).thenReturn(objectMapper);
        when(objectMapper.readValue(header, ClientContext.class)).thenReturn(clientContext);

        // when
        ClientContext actual = clientContextRetriever.getClientContext();

        // then
        assertEquals(clientContext, actual);
    }

    @Test
    void getClientContext_WithInvalidHeader_ThrowsException() {
        // given
        String header = "abc";
        when(requestAttributesSupplier.get()).thenReturn(servletRequestAttributes);
        when(servletRequestAttributes.getRequest()).thenReturn(request);
        when(request.getHeader("Client-context")).thenReturn(header);
        when(objectMapperSupplier.get()).thenReturn(new ObjectMapper());

        // when
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            clientContextRetriever.getClientContext();
        });
        assertEquals("Unable to parse Client-context", exception.getMessage());
    }
}
