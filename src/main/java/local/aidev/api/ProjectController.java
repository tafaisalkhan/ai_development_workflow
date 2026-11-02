package local.aidev.api;

import io.temporal.client.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import local.aidev.persistence.ProjectRepository;
import local.aidev.workspace.WorkspaceService;
import local.aidev.workflow.DevelopmentWorkflow;

import java.io.IOException;
import java.util.*;

import static local.aidev.model.Model.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final WorkflowClient client;
    private final ProjectRepository repository;
    private final WorkspaceService workspace;
    private final String queue;

    public ProjectController(WorkflowClient client, ProjectRepository repository, WorkspaceService workspace,
                             @Value("${app.temporal.task-queue}") String queue) {
        this.client = client;
        this.repository = repository;
        this.workspace = workspace;
        this.queue = queue;
    }

    @PostMapping("/start")
    public ResponseEntity<StartResponse> start(@Valid @RequestBody ProjectRequest request) throws IOException {
        String project = WorkspaceService.safeName(request.projectName());
        if (request.workspace() != null && !request.workspace().equals(project))
            throw new IllegalArgumentException("Workspace must match projectName");
        String id = "dev-" + project + "-" + UUID.randomUUID().toString().replace("-", "");
        repository.create(new ProjectRepository.ProjectMeta(id, project, project));
        repository.record(project, new DevelopmentStatus(id, project, "STARTED", null, null, 0, null), "STARTED", "Workflow requested");
        DevelopmentWorkflow workflow = client.newWorkflowStub(DevelopmentWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(id).setTaskQueue(queue).build());
        try {
            WorkflowClient.start(workflow::startDevelopment, request);
        } catch (RuntimeException e) {
            repository.record(project, new DevelopmentStatus(id, project, "START_FAILED", null, null, 0, e.getMessage()), "START_FAILED", e.getMessage());
            throw e;
        }
        return ResponseEntity.accepted().body(new StartResponse(id, project, "STARTED"));
    }

    @GetMapping("/{id}/status")
    public DevelopmentStatus status(@PathVariable String id) throws IOException {
        var meta = repository.get(id);
        try {
            DevelopmentStatus status = client.newWorkflowStub(DevelopmentWorkflow.class, id).getStatus();
            if (status != null) return status;
        } catch (RuntimeException ignored) {
        }
        return repository.status(meta.workspace());
    }

    @GetMapping("/{id}/files")
    public List<String> files(@PathVariable String id) throws IOException {
        return workspace.list(repository.get(id).workspace());
    }

    @GetMapping("/{id}/files/content")
    public ResponseEntity<String> content(@PathVariable String id, @RequestParam String path) throws IOException {
        if (path.startsWith(".platform/"))
            throw new IllegalArgumentException("Internal metadata is not a project artifact");
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(workspace.read(repository.get(id).workspace(), path));
    }

    @PostMapping("/{id}/decision")
    public Map<String, String> decision(@PathVariable String id, @Valid @RequestBody ApprovalDecision decision) throws IOException {
        repository.get(id);
        String result = client.newWorkflowStub(DevelopmentWorkflow.class, id).decide(decision);
        return Map.of("status", result, "approvalId", decision.approvalId());
    }

    @PostMapping("/{id}/cancel")
    public Map<String, String> cancel(@PathVariable String id) throws IOException {
        var meta = repository.get(id);
        WorkflowStub.fromTyped(client.newWorkflowStub(DevelopmentWorkflow.class, id)).cancel();
        repository.record(meta.workspace(), new DevelopmentStatus(id, meta.projectName(), "CANCELLED", null, null, 0, null), "CANCELLED", "Cancelled by API");
        return Map.of("status", "CANCELLED");
    }

    @GetMapping("/{id}/history")
    public List<HistoryEvent> history(@PathVariable String id) throws IOException {
        return repository.history(repository.get(id).workspace());
    }
}
