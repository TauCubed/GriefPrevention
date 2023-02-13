package me.ryanhamshire.GriefPrevention.util;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * This is a utility that shouldn't exist. By that, I mean it should not have to be written. <br>
 * Inside this boilerplate utility is a simple and self-explanatory method to recursively copy a directory and it's contents to another location. <br>
 * Because for some reason Java still doesn't have a simple one-liner to do this.
 * @author Tau
 */
public class FileUtils {

    public static void copyRecursive(Path from, Path to, CopyOption... options) throws IOException {
        try (Stream<Path> stream = Files.walk(from)) {
            stream.forEach(currentFrom -> {
                try {
                    copyFile(currentFrom, to.resolve(from.relativize(currentFrom)), options);
                } catch (IOException e) {
                    sneakyThrow(e);
                }
            });
        }
    }

    public static void copyFile(Path source, Path dest, CopyOption... options) throws IOException {
        try {
            Files.copy(source, dest, options);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    // well done java, don't let me throw checked exceptions in forEach. I think I'll do it anyway.
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

}
