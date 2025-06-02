package uk.gov.hmcts.reform.pcs.functional.testutils;

import io.vavr.collection.HashMap;

public class CurlLogger {
    public static void logCurlCommand(String method, String url, HashMap<String, String> headers) {
        StringBuilder curl = new StringBuilder("curl --location '" + url + "'");

        for (java.util.Map.Entry<String, String> header : headers.toJavaMap().entrySet()) {
            curl.append(" \\\n  --header '")
                .append(header.getKey())
                .append(": ")
                .append(header.getValue())
                .append("'");
        }

        if ("POST".equalsIgnoreCase(method)) {
            curl.append(" \\\n  --request POST");
        }

        System.out.println("\n========== Generated cURL ==========\n" + curl + "\n================================\n");
    }
}
