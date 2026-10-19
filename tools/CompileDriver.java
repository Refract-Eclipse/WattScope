import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** One-shot compiler for restricted Windows hosts; process exit releases archive handles. */
public final class CompileDriver {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A full JDK is required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, Locale.US, StandardCharsets.UTF_8);
        List<File> sources = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(args[2]), StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) sources.add(new File(line.replace("\"", "")));
        }
        Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromFiles(sources);
        List<String> options = Arrays.asList("-encoding", "UTF-8", "-source", "8", "-target", "8",
                "-bootclasspath", args[0], "-d", args[1]);
        boolean success = compiler.getTask(null, manager, diagnostics, options, null, units).call();
        for (Diagnostic<?> d : diagnostics.getDiagnostics()) System.err.println(d.getKind() + ": " + d.getMessage(Locale.US));
        // Some sandboxed Windows hosts allow reading jars but deny the real-path
        // lookup performed only when ZipFS closes them. This short-lived worker
        // lets the OS close handles after checking the actual compilation result.
        if (!success) System.exit(1);
        System.out.println("PASS: all Android Java sources compiled");
    }
}
