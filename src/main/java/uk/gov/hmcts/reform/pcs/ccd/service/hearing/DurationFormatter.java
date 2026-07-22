package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.springframework.stereotype.Component;

@Component
public class DurationFormatter {

    public String format(Integer hours, Integer minutes) {

        String result = "";

        if (hours != null) {
            if (hours == 1) {
                result = "1 hour";
            } else {
                result = "%d hours".formatted(hours);
            }
        }

        if (minutes != null) {
            if (!result.isEmpty()) {
                result += " ";
            }

            if (minutes == 1) {
                result += "1 minute";
            } else {
                result += "%d minutes".formatted(minutes);
            }
        }

        return result;
    }

}
