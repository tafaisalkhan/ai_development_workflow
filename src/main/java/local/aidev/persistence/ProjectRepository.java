package local.aidev.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import local.aidev.workspace.WorkspaceService;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static local.aidev.model.Model.*;

@Service
public class ProjectRepository {
    public record ProjectMeta(String workflowId, String projectName, String workspace) {
    }

    private final Path directory;
    private final WorkspaceService workspace;
    private final ObjectMapper mapper;

    public ProjectRepository(@Value("${app.workspace-root}") String root, WorkspaceService workspace, ObjectMapper mapper) throws IOException {
        this.directory = Path.of(root).toAbsolutePath().normalize().resolve(".platform");
        this.workspace = workspace;
        this.mapper = mapper;
        Files.createDirectories(directory);
        if (Files.isSymbolicLink(directory)) throw new IllegalArgumentException("Metadata directory is a link");
    }

    public synchronized void create(ProjectMeta meta) throws IOException {
        Path file = metadataFile(meta.workflowId());
        if (Files.exists(file)) throw new IllegalStateException("Project already exists");
        if (Files.exists(workspace.project(meta.workspace()), LinkOption.NOFOLLOW_LINKS))
            throw new IllegalStateException("Project workspace already exists");
        Files.createDirectory(workspace.project(meta.workspace()));
        Files.writeString(file, mapper.writeValueAsString(meta), StandardOpenOption.CREATE_NEW);
    }

    public ProjectMeta get(String id) throws IOException {
        Path p = metadataFile(id);
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) throw new NoSuchFileException(id);
        return mapper.readValue(Files.readString(p), ProjectMeta.class);
    }

    public WorkspaceService workspace() {
        return workspace;
    }

    private Path metadataFile(String id) {
        if (id == null || !id.matches("dev-[a-zA-Z0-9_-]{1,64}-[a-f0-9]{32}"))
            throw new IllegalArgumentException("Invalid workflow ID");
        return directory.resolve(id + ".json");
    }

    public void record(String project, DevelopmentStatus status, String action, String detail) throws IOException {
        synchronized (project.intern()) {
            workspace.writeJson(project, ".platform/status.json", status);
            List<HistoryEvent> history;
            try {
                history = mapper.readValue(workspace.read(project, ".platform/history.json"), new TypeReference<>() {
                });
            } catch (NoSuchFileException e) {
                history = new ArrayList<>();
            }
            String stage = status.stage() == null ? "" : status.stage().name();
            String eventDetail = "revision=" + status.revision() + "; " + detail;
            if (!history.isEmpty()) {
                HistoryEvent last = history.getLast();
                if (last.stage().equals(stage) && last.action().equals(action) && last.detail().equals(eventDetail))
                    return;
            }
            history.add(new HistoryEvent(java.time.Instant.now().toString(), stage, action, eventDetail));
            workspace.writeJson(project, ".platform/history.json", history);
        }
    }

    public DevelopmentStatus status(String project) throws IOException {
        return workspace.readJson(project, ".platform/status.json", DevelopmentStatus.class);
    }

    public List<HistoryEvent> history(String project) throws IOException {
        return mapper.readValue(workspace.read(project, ".platform/history.json"), new TypeReference<>() {
        });
    }
}
