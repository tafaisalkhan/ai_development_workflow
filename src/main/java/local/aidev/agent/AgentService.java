package local.aidev.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import local.aidev.workspace.WorkspaceService;

import java.io.IOException;
import java.util.*;

import static local.aidev.model.Model.*;

@Service
public class AgentService {
    private final ChatClient client;
    private final WorkspaceService workspace;
    private final ObjectMapper mapper;

    public AgentService(ChatClient.Builder builder, WorkspaceService workspace, ObjectMapper mapper) {
        this.client = builder.build();
        this.workspace = workspace;
        this.mapper = mapper;
    }

    public ArtifactRef generate(String project, Stage stage, String prompt, String feedback) throws IOException {
        String requirements = readIfPresent(project, "docs/requirements.md");
        String architecture = readIfPresent(project, "docs/architecture.md");
        String review = readIfPresent(project, "docs/review.md");
        String sources = sourceSnapshot(project);
        String context = "Original request:\n" + prompt + "\nRevision feedback:\n" + (feedback == null ? "" : feedback)
                + "\nApproved requirements:\n" + requirements + "\nApproved architecture:\n" + architecture
                + "\nExisting source files:\n" + sources + "\nReview findings:\n" + review;
        return switch (stage) {
            case REQUIREMENTS ->
                    document(project, "docs/requirements.md", "You are a business analyst. Write Markdown with functional and nonfunctional requirements, user stories, acceptance criteria, assumptions and exclusions. Preserve the user's intent and avoid inventing requirements.", context);
            case ARCHITECTURE ->
                    document(project, "docs/architecture.md", "You are a software architect. Write Markdown covering technology decisions, components, project structure, data flow, APIs if applicable, security and testing strategy. Follow approved requirements.", context);
            case DEVELOPMENT -> develop(project, context);
            case REVIEW ->
                    document(project, "docs/review.md", "You are a code reviewer. Inspect the actual source files supplied. Write Markdown findings with severity, missing requirements, security and maintainability concerns, and corrections. Do not claim to have executed code.", context);
            case QA -> qa(project, context);
        };
    }

    private ArtifactRef document(String project, String path, String system, String context) throws IOException {
        String content = client.prompt().system(system).user(context).call().content();
        if (content == null || content.isBlank()) throw new IllegalStateException("AI returned an empty document");
        return new ArtifactRef(path, workspace.write(project, path, content));
    }

    private ArtifactRef develop(String project, String context) throws IOException {
        String answer = client.prompt().system("You are a software developer. Return ONLY a JSON object of the form {\"files\":[{\"path\":\"relative/path\",\"content\":\"complete file contents\"}]}. Generate a complete runnable project from the approved requirements and architecture, including a README with local run instructions. Choose filenames dynamically. Do not include docs/ approval documents or .platform files. No markdown fences.").user(context).call().content();
        if (answer == null) throw new IllegalStateException("AI returned no files");
        GeneratedFiles generated = mapper.readValue(answer, GeneratedFiles.class);
        if (generated.files() == null || generated.files().isEmpty() || generated.files().size() > 100)
            throw new IllegalArgumentException("Invalid generated file count");
        Set<String> seen = new HashSet<>();
        for (GeneratedFile file : generated.files()) {
            if (file.path() == null || file.content() == null || file.path().toLowerCase(Locale.ROOT).matches("^(docs|\\.platform)(/.*)?$") || !seen.add(file.path().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Invalid generated file instruction");
            workspace.resolve(project, file.path());
            if (file.content().length() > 2_000_000) throw new IllegalArgumentException("Generated file too large");
        }
        for (GeneratedFile file : generated.files()) workspace.write(project, file.path(), file.content());
        String manifest = mapper.writeValueAsString(generated.files().stream().map(GeneratedFile::path).toList());
        return new ArtifactRef(".platform/generated-files.json", workspace.write(project, ".platform/generated-files.json", manifest));
    }

    private ArtifactRef qa(String project, String context) throws IOException {
        List<String> files = workspace.list(project);
        StringBuilder results = new StringBuilder("# QA report\n\n## Checks actually performed\n\n");
        results.append("- Project file inventory: ").append(files.size()).append(" files found.\n");
        for (String path : files) {
            String content = workspace.read(project, path);
            results.append("- ").append(path).append(": ").append(content.isBlank() ? "FAIL (empty)" : "PASS (nonempty)").append(".\n");
        }
        results.append("\nNo generated-project test suite was required or executed. Browser and Maven test results are unverified.\n\n");
        String analysis = client.prompt().system("You are a QA analyst. Produce Markdown test cases and requirement coverage based on supplied requirements and source. Clearly label every unexecuted test as NOT RUN. Never claim a test passed unless the supplied executed check proves it.").user(context + "\nActual checks:\n" + results).call().content();
        if (analysis != null) results.append(analysis);
        return new ArtifactRef("docs/qa-report.md", workspace.write(project, "docs/qa-report.md", results.toString()));
    }

    private String readIfPresent(String project, String path) throws IOException {
        try {
            return workspace.read(project, path);
        } catch (java.nio.file.NoSuchFileException e) {
            return "";
        }
    }

    private String sourceSnapshot(String project) throws IOException {
        StringBuilder result = new StringBuilder();
        for (String file : workspace.list(project)) {
            if (file.startsWith("docs/")) continue;
            String content = workspace.read(project, file);
            result.append("\n### ").append(file).append("\n").append(content, 0, Math.min(content.length(), 30_000)).append("\n");
            if (result.length() > 150_000) break;
        }
        return result.toString();
    }
}
