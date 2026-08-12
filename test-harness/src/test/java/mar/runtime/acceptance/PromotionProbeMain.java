package mar.runtime.acceptance;

import mar.runtime.groovy.GroovyRuntime;
import mar.runtime.io.EvalOutputCapture;
import mar.runtime.io.ThreadLocalPrintRouter;
import mar.runtime.session.RuntimeSession;
import mar.runtime.session.SessionManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class PromotionProbeMain {
    private PromotionProbeMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected <project-root> <tool-source>");
        }
        Path projectRoot = Path.of(args[0]).toAbsolutePath().normalize();
        Path source = Path.of(args[1]).toAbsolutePath().normalize();
        Path target = projectRoot.resolve(
                ".minecraft-agent-runtime/tools/loader/class-visibility.groovy");
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        ThreadLocalPrintRouter router = ThreadLocalPrintRouter.install();
        try (GroovyRuntime groovy = new GroovyRuntime();
             SessionManager sessions = new SessionManager(
                     projectRoot, groovy, new EvalOutputCapture(router))) {
            RuntimeSession session = sessions.defaultSession();
            Object loaded = session.evaluate(
                    "runtime.tools.reloadPath('loader/class-visibility.groovy').ok");
            Object resolved = session.evaluate(
                    "vars.tools.loader.'class-visibility'.resolve('java.lang.String').name");
            Object absent = session.evaluate(
                    "vars.tools.loader.'class-visibility'.isPresent('mar.fixture.DeliberatelyMissing')");
            if (!Boolean.TRUE.equals(loaded)
                    || !"java.lang.String".equals(resolved)
                    || !Boolean.FALSE.equals(absent)) {
                throw new IllegalStateException(
                        "Promotion probe failed: " + loaded + ", " + resolved + ", " + absent);
            }
            System.out.println("PROMOTION_OK pid=" + ProcessHandle.current().pid());
        } finally {
            router.close();
        }
    }
}
