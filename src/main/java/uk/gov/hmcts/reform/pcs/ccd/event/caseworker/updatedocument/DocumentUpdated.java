package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.updatedocument;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventMetadata;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentCategoryUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentCategoryUpdateService.DocumentMove;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DocumentMoveRoles.DOCUMENT_MOVE_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.labelFor;

@Component
@RequiredArgsConstructor
public class DocumentUpdated implements CCDConfig<PCSCase, State, UserRole> {

    private final DocumentCategoryUpdateService documentCategoryUpdateService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.decentralisedEvent(EventId.DocumentUpdated.name(), this::submit)
            .forAllStates()
            .name("Document updated")
            .grantHistoryOnly(DOCUMENT_MOVE_ROLES);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
        List<DocumentMove> moves =
            documentCategoryUpdateService.updateCategories(payload.caseReference(), payload.caseData());
        if (moves.isEmpty()) {
            return SubmitResponse.defaultResponse();
        }

        // Shown in History as the event's Summary and Comment
        String summary = moves.size() == 1
            ? "Document moved to " + labelFor(moves.getFirst().toCategory())
            : moves.size() + " documents moved";
        String description = moves.stream()
            .map(move -> "%s: %s → %s".formatted(
                move.fileName(), labelFor(move.fromCategory()), labelFor(move.toCategory())))
            .collect(Collectors.joining("\n"));

        return SubmitResponse.<State>builder()
            .eventMetadata(EventMetadata.builder().summary(summary).description(description).build())
            .build();
    }
}
