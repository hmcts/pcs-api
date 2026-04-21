package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocument {

    @CCD(access = {CitizenAccess.class})
    private Document document;

    @CCD(access = {CitizenAccess.class})
    private String contentType;

    @CCD(access = {CitizenAccess.class})
    private Long size;
}
