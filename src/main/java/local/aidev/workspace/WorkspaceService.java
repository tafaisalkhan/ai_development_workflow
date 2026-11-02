package local.aidev.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

@Service
public class WorkspaceService {
    private final Path root;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public WorkspaceService(@Value("${app.workspace-root}") String root, ObjectMapper mapper) throws IOException {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.mapper = mapper;
        Files.createDirectories(this.root);
        if (Files.isSymbolicLink(this.root)) throw new IllegalArgumentException("Workspace root is a link");
    }

    public static String safeName(String name) {
        if (name == null || !name.matches("[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}") || name.matches("(?i)con|prn|aux|nul|com[1-9]|lpt[1-9]"))
            throw new IllegalArgumentException("Unsafe project name");
        return name;
    }

    public Path project(String name) throws IOException {
        Path p = root.resolve(safeName(name));
        checkExistingParents(p);
        return p;
    }

    private void checkExistingParents(Path path) throws IOException {
        if (!path.normalize().startsWith(root)) throw new IllegalArgumentException("Path escapes workspace");
        for (Path p = root; p != null && path.startsWith(p); ) {
            if (Files.exists(p, NOFOLLOW_LINKS) && (Files.isSymbolicLink(p) || !p.toRealPath().startsWith(root.toRealPath())))
                throw new IllegalArgumentException("Linked or escaped workspace path");
            if (p.equals(path)) break;
            Path relative = root.relativize(path);
            int index = root.relativize(p).getNameCount();
            p = root.resolve(relative.subpath(0, Math.min(index + 1, relative.getNameCount())));
        }
    }

    public Path resolve(String project, String relative) throws IOException {
        if (relative == null || relative.isBlank() || relative.contains("\\") || relative.matches("(?i)^[a-z]:.*"))
            throw new IllegalArgumentException("Unsafe file path");
        Path rel = Path.of(relative);
        if (rel.isAbsolute() || rel.toString().contains(":") || java.util.stream.StreamSupport.stream(rel.spliterator(), false).anyMatch(part -> part.toString().equals("..") || part.toString().equals(".")))
            throw new IllegalArgumentException("Unsafe file path");
        Path target = project(project).resolve(rel).normalize();
        if (!target.startsWith(project(project))) throw new IllegalArgumentException("Path escapes project");
        checkExistingParents(target);
        return target;
    }

    public void create(String project) throws IOException {
        synchronized (locks.computeIfAbsent(project, k -> new Object())) {
            Files.createDirectories(project(project));
        }
    }

    public String write(String project, String relative, String content) throws IOException {
        if (content == null || content.length() > 2_000_000)
            throw new IllegalArgumentException("File content exceeds limit");
        synchronized (locks.computeIfAbsent(project, k -> new Object())) {
            Path target = resolve(project, relative);
            Files.createDirectories(target.getParent());
            checkExistingParents(target);
            Path temp = Files.createTempFile(target.getParent(), ".ai-", ".tmp");
            try {
                Files.writeString(temp, content);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
            return hash(content);
        }
    }

    public String read(String project, String relative) throws IOException {
        Path p = resolve(project, relative);
        if (!Files.isRegularFile(p, NOFOLLOW_LINKS)) throw new NoSuchFileException(relative);
        return Files.readString(p);
    }

    public List<String> list(String project) throws IOException {
        Path p = project(project);
        if (!Files.isDirectory(p, NOFOLLOW_LINKS)) return List.of();
        try (var stream = Files.walk(p)) {
            return stream.filter(f -> Files.isRegularFile(f, NOFOLLOW_LINKS)).map(f -> p.relativize(f).toString().replace('\\', '/')).filter(f -> !f.startsWith(".platform/")).sorted().toList();
        }
    }

    public void writeJson(String project, String relative, Object value) throws IOException {
        write(project, relative, mapper.writeValueAsString(value));
    }

    public <T> T readJson(String project, String relative, Class<T> type) throws IOException {
        return mapper.readValue(read(project, relative), type);
    }

    private String hash(String s) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(b);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
