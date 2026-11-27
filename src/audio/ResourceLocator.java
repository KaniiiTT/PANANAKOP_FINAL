package audio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Finds audio resources the same way the rest of the game finds its images and levels
 * ({@code utilz.LoadSave}): first on the classpath, then in the project's {@code res/} folder on disk.
 *
 * <p>This matters because the project has no build file. When it is run from an IDE, {@code res/} is often NOT
 * on the classpath, and a classpath-only lookup silently finds nothing (no manifest, no music).
 */
final class ResourceLocator {
	private static Path resFolder;       // cached once found
	private static boolean searched;

	private ResourceLocator() {}

	/** Opens e.g. "/audio/music/x.wav"; returns null if it exists neither on the classpath nor under res/. */
	static InputStream open(String classpathPath) throws IOException {
		InputStream in = ResourceLocator.class.getResourceAsStream(classpathPath);
		if (in != null)
			return in;
		Path res = resFolder();
		if (res == null)
			return null;
		Path file = res.resolve(classpathPath.startsWith("/") ? classpathPath.substring(1) : classpathPath);
		return Files.isRegularFile(file) ? Files.newInputStream(file) : null;
	}

	/** Human-readable place the resources are read from (for the start-up log line). */
	static String describeSource(String classpathPath) {
		if (ResourceLocator.class.getResource(classpathPath) != null)
			return "classpath";
		Path res = resFolder();
		return res == null ? "NOT FOUND" : res.toString();
	}

	/** Looks for a folder named "res" that contains an "audio" folder. Searched once, then cached. */
	private static synchronized Path resFolder() {
		if (searched)
			return resFolder;
		searched = true;
		// 1) the working directory and its parents (how an IDE normally launches the game)
		resFolder = searchUp(Paths.get("").toAbsolutePath().normalize());
		// 2) where the compiled classes live (works even if the working directory is somewhere else)
		if (resFolder == null) {
			try {
				Path code = Paths.get(ResourceLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
				resFolder = searchUp(Files.isRegularFile(code) ? code.getParent() : code);
			} catch (URISyntaxException | RuntimeException e) {
				// no code source: fall through
			}
		}
		// 3) the outer repository folder opened in the IDE (same fallback LoadSave has)
		if (resFolder == null) {
			Path start = Paths.get("").toAbsolutePath().normalize();
			try (Stream<Path> paths = Files.walk(start, 4)) {
				resFolder = paths.filter(Files::isDirectory)
						.filter(p -> p.getFileName() != null && p.getFileName().toString().equals("res"))
						.filter(p -> Files.isDirectory(p.resolve("audio")))
						.findFirst().orElse(null);
			} catch (IOException | RuntimeException e) {
				// leave null
			}
		}
		return resFolder;
	}

	private static Path searchUp(Path directory) {
		for (int i = 0; i < 8 && directory != null; i++) {
			Path candidate = directory.resolve("res");
			if (Files.isDirectory(candidate.resolve("audio")))
				return candidate;
			directory = directory.getParent();
		}
		return null;
	}
}
