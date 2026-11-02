package local.aidev.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import static local.aidev.model.Model.*;

@ActivityInterface
public interface DevelopmentActivities {
    @ActivityMethod
    ArtifactRef runStage(String project, Stage stage, String prompt, String feedback);

    @ActivityMethod
    void record(String project, DevelopmentStatus status, String action, String detail);
}
