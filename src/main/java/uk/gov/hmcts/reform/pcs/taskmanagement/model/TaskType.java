package uk.gov.hmcts.reform.pcs.taskmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskType {

    CHECK_MULTIPLE_DEFENDANTS("Review multiple defendants");

    private final String name;

}
