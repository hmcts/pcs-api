package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import org.springframework.context.annotation.Profile;

@Profile({"local", "dev", "preview"})
public class SupportException extends RuntimeException  {

    public SupportException(Throwable cause) {
        super(cause);
    }

    public SupportException(String message, Throwable cause) {
        super(message, cause);
    }

}
