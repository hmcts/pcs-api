package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@RequiredArgsConstructor
@Getter
public enum State {

    @CCD(
        label = "Open",
        access = {DefaultStateAccess.class}
    )
    Open;
}

