package local.aidev.model;

import jakarta.validation.constraints.*;

import java.util.*;

public final class Model {
    private Model() {
    }

    public record ProjectRequest(@NotBlank @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}") String projectName,
                                 @NotBlank @Size(max = 50000) String prompt, List<String> technology,
                                 boolean approvalRequired,
                                 @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}") String workspace) {
    }

    public enum Stage {REQUIREMENTS, ARCHITECTURE, DEVELOPMENT, REVIEW, QA}

    public enum Decision {APPROVE, REVISE, REJECT}

    public record ApprovalDecision(@NotBlank String approvalId, @NotNull Stage stage,
                                   @NotNull Decision decision, String feedback) {
    }

    public record DevelopmentStatus(String workflowId, String projectName, String status,
                                    Stage stage, String approvalId, int revision, String error) {
    }

    public record HistoryEvent(String at, String stage, String action, String detail) {
    }

    public record ArtifactRef(String path, String sha256) {
    }

    public record GeneratedFile(String path, String content) {
    }

    public record GeneratedFiles(List<GeneratedFile> files) {
    }

    public record StartResponse(String workflowId, String projectName, String status) {
    }
}
