package uk.gov.hmcts.reform.pcs.ccd.service;

import java.util.Collection;

public record UserRoles(String userId, Collection<String> roles) {
}
