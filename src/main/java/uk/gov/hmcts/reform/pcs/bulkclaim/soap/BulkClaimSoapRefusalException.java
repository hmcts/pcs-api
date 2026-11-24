package uk.gov.hmcts.reform.pcs.bulkclaim.soap;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/** A well-formed request the service refuses as a whole, reported as a Client SOAP fault. */
@SoapFault(faultCode = FaultCode.CLIENT)
public class BulkClaimSoapRefusalException extends RuntimeException {

    public BulkClaimSoapRefusalException(String message) {
        super(message);
    }
}
