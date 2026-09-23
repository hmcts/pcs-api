package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MultiMediaFileTypes {

    MP3("mp3"),
    MP4("mp4"),
    M4A("m4a"),
    MPEG("mpeg");

    private final String fileType;
}
