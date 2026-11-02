package local.aidev.workflow;

import io.temporal.workflow.*;

import static local.aidev.model.Model.*;

@WorkflowInterface
public interface DevelopmentWorkflow {
    @WorkflowMethod
    String startDevelopment(ProjectRequest request);

    @SignalMethod
    void submitApproval(ApprovalDecision decision);

    @UpdateMethod
    String decide(ApprovalDecision decision);

    @UpdateValidatorMethod(updateName = "decide")
    void validateDecision(ApprovalDecision decision);

    @QueryMethod
    DevelopmentStatus getStatus();
}
