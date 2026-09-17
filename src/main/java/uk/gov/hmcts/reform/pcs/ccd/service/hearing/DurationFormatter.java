package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DurationFormatter {

    public String format(Integer days, Integer hours, Integer minutes) {
        List<String> durationParts = new ArrayList<>();

        if (days != null && days > 0) {
            durationParts.add(buildDurationLabel(days, "day"));
        }
        if (hours != null) {
            durationParts.add(buildDurationLabel(hours, "hour"));
        }
        if (minutes != null) {
            durationParts.add(buildDurationLabel(minutes, "minute"));
        }

        return String.join(" ", durationParts);
    }

    private String buildDurationLabel(int value, String label) {
        String suffix = value == 1 ? "" : "s";
        return value + " " + label + suffix;
    }

}
