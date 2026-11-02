package local.aidev.activity;

import io.temporal.activity.Activity;
import local.aidev.agent.AgentService;
import local.aidev.persistence.ProjectRepository;
import org.springframework.stereotype.Component;
import java.nio.file.NoSuchFileException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static local.aidev.model.Model.*;

@Component
public class DevelopmentActivitiesImpl implements DevelopmentActivities {
    private final AgentService agents;
    private final ProjectRepository repository;
    public DevelopmentActivitiesImpl(AgentService agents, ProjectRepository repository) { this.agents = agents; this.repository = repository; }
    @Override public ArtifactRef runStage(String project, Stage stage, String prompt, String feedback) {
        try {
            String activityId = Activity.getExecutionContext().getInfo().getActivityId();
            String marker = ".platform/activities/" + UUID.nameUUIDFromBytes(activityId.getBytes(StandardCharsets.UTF_8)) + ".json";
            try { return repository.workspace().readJson(project, marker, ArtifactRef.class); }
            catch (NoSuchFileException ignored) { }
            ArtifactRef result = agents.generate(project, stage, prompt, feedback);
            repository.workspace().writeJson(project, marker, result);
            return result;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    @Override public void record(String project, DevelopmentStatus status, String action, String detail) {
        try { repository.record(project, status, action, detail); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
