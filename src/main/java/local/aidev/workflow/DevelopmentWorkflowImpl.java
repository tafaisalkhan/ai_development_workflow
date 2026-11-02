package local.aidev.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import local.aidev.activity.DevelopmentActivities;

import java.time.Duration;

import static local.aidev.model.Model.*;

public class DevelopmentWorkflowImpl implements DevelopmentWorkflow {
    private final DevelopmentActivities activities = Workflow.newActivityStub(DevelopmentActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10))
                    .setRetryOptions(RetryOptions.newBuilder().setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2).setMaximumInterval(Duration.ofMinutes(1)).setMaximumAttempts(4).build()).build());
    private DevelopmentStatus status;
    private ApprovalDecision pending;
    private final int maxRevisions;

    public DevelopmentWorkflowImpl(int maxRevisions) {
        this.maxRevisions = maxRevisions;
    }

    @Override
    public String startDevelopment(ProjectRequest request) {
        String id = Workflow.getInfo().getWorkflowId();
        status = new DevelopmentStatus(id, request.projectName(), "RUNNING", null, null, 0, null);
        try {
            String feedback = "";
            for (Stage stage : Stage.values()) {
                int revision = 0;
                while (true) {
                    status = new DevelopmentStatus(id, request.projectName(), "RUNNING", stage, null, revision, null);
                    activities.record(request.projectName(), status, "STAGE_STARTED", stage.name());
                    String specification = request.prompt() + "\nRequested technology: " + (request.technology() == null ? "unspecified" : request.technology());
                    ArtifactRef artifact = activities.runStage(request.projectName(), stage, specification, feedback);
                    String approvalId = stage.name().toLowerCase() + "-v" + (revision + 1);
                    status = new DevelopmentStatus(id, request.projectName(), request.approvalRequired() ? "WAITING_APPROVAL" : "RUNNING", stage,
                            request.approvalRequired() ? approvalId : null, revision, null);
                    activities.record(request.projectName(), status, "ARTIFACT_CREATED", artifact.path());
                    if (!request.approvalRequired()) break;
                    activities.record(request.projectName(), status, "APPROVAL_REQUESTED", approvalId);
                    Workflow.await(() -> pending != null);
                    ApprovalDecision decision = pending;
                    pending = null;
                    activities.record(request.projectName(), status, decision.decision().name(), decision.feedback() == null ? "" : decision.feedback());
                    if (decision.decision() == Decision.REJECT) {
                        status = new DevelopmentStatus(id, request.projectName(), "REJECTED", stage, null, revision, null);
                        activities.record(request.projectName(), status, "REJECTED", stage.name());
                        return "REJECTED";
                    }
                    if (decision.decision() == Decision.APPROVE) break;
                    if (++revision > maxRevisions) {
                        status = new DevelopmentStatus(id, request.projectName(), "REVISION_LIMIT", stage, null, revision, "Maximum revisions reached");
                        activities.record(request.projectName(), status, "REVISION_LIMIT", stage.name());
                        return "REVISION_LIMIT";
                    }
                    feedback = decision.feedback();
                }
                feedback = "";
            }
            status = new DevelopmentStatus(id, request.projectName(), "COMPLETED", Stage.QA, null, 0, null);
            activities.record(request.projectName(), status, "COMPLETED", "Project completed");
            return "COMPLETED";
        } catch (Exception e) {
            status = new DevelopmentStatus(id, request.projectName(), "FAILED", status.stage(), null, status.revision(), e.getMessage());
            activities.record(request.projectName(), status, "FAILED", e.getMessage());
            throw e;
        }
    }

    @Override
    public void submitApproval(ApprovalDecision decision) {
        try {
            validateDecision(decision);
            pending = decision;
        } catch (IllegalArgumentException ignored) { /* Signals cannot acknowledge rejection; REST uses Updates. */ }
    }

    @Override
    public String decide(ApprovalDecision decision) {
        pending = decision;
        return "ACCEPTED";
    }

    @Override
    public void validateDecision(ApprovalDecision decision) {
        if (status == null || !"WAITING_APPROVAL".equals(status.status()) || pending != null || decision == null ||
                decision.stage() != status.stage() || !status.approvalId().equals(decision.approvalId()))
            throw new IllegalArgumentException("Stale, duplicate or mismatched approval");
        if (decision.decision() == Decision.REVISE && (decision.feedback() == null || decision.feedback().isBlank()))
            throw new IllegalArgumentException("Revision feedback is required");
    }

    @Override
    public DevelopmentStatus getStatus() {
        return status;
    }
}
