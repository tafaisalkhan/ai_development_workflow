package local.aidev.workspace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceServiceTest {
    @TempDir Path root;
    @Test void writesOnlyInsideProject() throws Exception {
        WorkspaceService service = new WorkspaceService(root.toString(), new ObjectMapper());
        service.create("sample");
        service.write("sample", "src/Main.java", "class Main {}");
        assertEquals("class Main {}", service.read("sample", "src/Main.java"));
        assertThrows(IllegalArgumentException.class, () -> service.write("sample", "../escape.txt", "bad"));
        assertThrows(IllegalArgumentException.class, () -> service.write("sample", "src/../../escape.txt", "bad"));
    }
    @Test void rejectsLinkedDirectory() throws Exception {
        WorkspaceService service = new WorkspaceService(root.toString(), new ObjectMapper());
        service.create("sample");
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path link = root.resolve("sample").resolve("link");
        try { Files.createSymbolicLink(link, outside); }
        catch (UnsupportedOperationException | java.nio.file.FileSystemException e) { return; }
        assertThrows(IllegalArgumentException.class, () -> service.write("sample", "link/file.txt", "bad"));
    }
}
