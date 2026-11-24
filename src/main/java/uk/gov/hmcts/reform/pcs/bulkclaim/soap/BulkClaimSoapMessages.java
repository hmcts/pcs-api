package uk.gov.hmcts.reform.pcs.bulkclaim.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The SOAP wrapper elements of the bulk claim service. Each carries a single string: the v2 XML document in the
 * request, the v2 response document in the response. Names and namespace are those of PCOL's BulkClaim service
 * so that a client generated from its WSDL keeps working.
 */
public final class BulkClaimSoapMessages {

    public static final String NAMESPACE = "http://impl.service.bulkclaim.webservice.pcol.dca.gov.uk";

    private BulkClaimSoapMessages() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlRootElement(name = "processRentalClaim", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProcessRentalClaim {
        @XmlElement(namespace = NAMESPACE)
        private String rentalRequest;
        @XmlElement(namespace = NAMESPACE)
        private Boolean validateOnly;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlRootElement(name = "processRentalClaimResponse", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProcessRentalClaimResponse {
        @XmlElement(namespace = NAMESPACE)
        private String processRentalClaimReturn;

        public ProcessRentalClaimResponse(String processRentalClaimReturn) {
            this.processRentalClaimReturn = processRentalClaimReturn;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlRootElement(name = "processMortgageClaim", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProcessMortgageClaim {
        @XmlElement(namespace = NAMESPACE)
        private String mortgageRequest;
        @XmlElement(namespace = NAMESPACE)
        private Boolean validateOnly;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlRootElement(name = "processMortgageClaimResponse", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProcessMortgageClaimResponse {
        @XmlElement(namespace = NAMESPACE)
        private String processMortgageClaimReturn;

        public ProcessMortgageClaimResponse(String processMortgageClaimReturn) {
            this.processMortgageClaimReturn = processMortgageClaimReturn;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlRootElement(name = "getClaimStatus", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GetClaimStatus {
        @XmlElement(namespace = NAMESPACE)
        private String customerUniqueIdentifier;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlRootElement(name = "getClaimStatusResponse", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GetClaimStatusResponse {
        @XmlElement(namespace = NAMESPACE)
        private String getClaimStatusReturn;

        public GetClaimStatusResponse(String getClaimStatusReturn) {
            this.getClaimStatusReturn = getClaimStatusReturn;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlRootElement(name = "getClaimStatuses", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GetClaimStatuses {
        @XmlElement(namespace = NAMESPACE)
        private String statusRequest;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @XmlRootElement(name = "getClaimStatusesResponse", namespace = NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GetClaimStatusesResponse {
        @XmlElement(namespace = NAMESPACE)
        private String getClaimStatusesReturn;

        public GetClaimStatusesResponse(String getClaimStatusesReturn) {
            this.getClaimStatusesReturn = getClaimStatusesReturn;
        }
    }
}
