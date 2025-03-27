package uk.gov.hmcts.reform.pcs.postalcode.dto;

import lombok.Data;

@Data
public class PostCodeResponse {

    private String ePIMSId;

    public PostCodeResponse(String ePIMSId) {
        this.ePIMSId = ePIMSId;
    }

    public PostCodeResponse() {
    }
}
