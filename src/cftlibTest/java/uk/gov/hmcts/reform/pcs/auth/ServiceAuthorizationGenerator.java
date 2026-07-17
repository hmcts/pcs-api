package uk.gov.hmcts.reform.pcs.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;

public class ServiceAuthorizationGenerator {

    public static String generateTestS2SToken(String serviceName) {
        return JWT.create().withSubject(serviceName).withIssuedAt(new Date()).sign(Algorithm.HMAC256("secret"));
    }

}
