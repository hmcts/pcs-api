package uk.gov.hmcts.reform.pcs.ccd.service.party;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequestScope
public class RequestHolder {

    @Autowired
    private HttpServletRequest request;

    public HttpServletRequest getRequest() {
        return ((ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes()
        ).getRequest();
    }

    public  String getHeader(String name) {
        return getRequest().getHeader(name);
    }

}
