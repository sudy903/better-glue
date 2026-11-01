import com.sun.source.util.JavacTask;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 只做语法解析（不解析符号），用于在没有 Minecraft 依赖的环境里给源码做结构检查。
 */
public final class ParseCheck {

    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        List<File> files = new ArrayList<>();
        for (String path : args) {
            files.add(new File(path));
        }

        JavacTask task = (JavacTask) compiler.getTask(
            null, fileManager, diagnostics, List.of("-proc:none"), null,
            fileManager.getJavaFileObjectsFromFiles(files)
        );
        task.parse();

        int errors = 0;
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                errors++;
                System.out.println("ERROR " + d.getSource().getName() + ":" + d.getLineNumber() + " " + d.getMessage(null));
            }
        }
        System.out.println("files=" + files.size() + " parse-errors=" + errors);
        fileManager.close();
        if (errors > 0) {
            System.exit(1);
        }
    }
}
