import java.io.IOException;
import java.io.*;


public class createIdamUser {
    public static void main(String[] args) {
        // Complex JSON with nested braces
        String jsonPayload = """
            {"user": {
                    "id":"id",
                    "email":"email",
                    "forename":"fn_d8d8ad80-84d9-4416-95ea-bce1a6eb1247",
                    "surname":"sn_d8d8ad80-84d9-4416-95ea-bce1a6eb1247",
                    "roleNames": [
                        "citizen"
                    ]
                }}""";

        String outputFile = "response.json";

        // Properly escaped curl command
        String[] curlCommand = {
            "curl",
            "-X", "POST",
            "-H", "Authorization: Bearer eyJ0eXAiOiJKV1Q...",
            "-H", "Content-Type: application/json",
            "-d", jsonPayload,
            "https://idam-testing-support-api.aat.platform.hmcts.net/test/idam/users"
        };
        try {
            Process process = new ProcessBuilder(curlCommand).start();

            // Save output to file
            try (InputStream inputStream = process.getInputStream();
                 FileOutputStream fileOutput = new FileOutputStream(outputFile)) {
                inputStream.transferTo(fileOutput);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
