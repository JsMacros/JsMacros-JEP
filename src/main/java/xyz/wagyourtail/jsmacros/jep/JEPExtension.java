package xyz.wagyourtail.jsmacros.jep;

import com.google.common.collect.Sets;
import jep.JepAccess;
import jep.JepException;
import jep.MainInterpreter;
import jep.SubInterpreter;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.extensions.Extension;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.jep.config.JEPConfig;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.jep.library.impl.FWrapper;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class JEPExtension implements Extension {

    private static JEPLanguageDefinition languageDescription;
    @Override
    public void init() {
    
        try {
            Core.getInstance().config.addOptions("jep", JEPConfig.class);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }

        addSharedLibrary(Core.getInstance().config.getOptions(JEPConfig.class).path);
        preInit();
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getLanguageImplName() {
        return "jep";
    }

    @Override
    public ExtMatch extensionMatch(File file) {
        if (file.getName().endsWith(".py")) {
            if (file.getName().contains(getLanguageImplName())) {
                return ExtMatch.MATCH_WITH_NAME;
            } else {
                return ExtMatch.MATCH;
            }
        }
        return ExtMatch.NOT_MATCH;
    }

    @Override
    public String defaultFileExtension() {
        return "py";
    }

    @Override
    public BaseLanguage<?, ?> getLanguage(Core<?, ?> core) {
        if (languageDescription == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(JEPExtension.class.getClassLoader());
            languageDescription = new JEPLanguageDefinition(this, core);
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return languageDescription;
    }

    @Override
    public Set<Class<? extends BaseLibrary>> getLibraries() {
        return Sets.newHashSet(FWrapper.class);
    }
    @Override
    public BaseWrappedException<?> wrapException(Throwable ex) {
        if (ex instanceof JepException) {
            Throwable cause = ex.getCause();
            String message;
            if (cause != null) {
                message = cause.getClass().getName();
                String intMessage = cause.getMessage();
                if (intMessage != null) {
                    message += ": " + intMessage;
                }
            }
            else {
                message = ex.getMessage();
                message = message.split("'")[1] + ": " + message.split(":", 2)[1];
            }
            Iterator<StackTraceElement> elements = Arrays.stream(ex.getStackTrace()).iterator();
            return new BaseWrappedException<>(ex, message, null, elements.hasNext() ? wrapStackTrace(elements.next(), elements) : null);
        }
        return null;
    }

    private BaseWrappedException<?> wrapStackTrace(StackTraceElement current, Iterator<StackTraceElement> elements) {
        if (current.isNativeMethod()) return null;
        String fileName = current.getFileName();
        if (fileName == null || fileName.endsWith(".java")) {
            return BaseWrappedException.wrapHostElement(current, elements.hasNext() ? wrapStackTrace(elements.next(), elements) : null);
        }
        File folder = new File(current.getClassName()).getParentFile();
        BaseWrappedException.SourceLocation loc = new BaseWrappedException.GuestLocation(new File(folder, fileName), -1, -1, current.getLineNumber(), -1);
        String message = current.getMethodName();
        return new BaseWrappedException<>(current, " at " + message, loc, elements.hasNext() ? wrapStackTrace(elements.next(), elements) : null);
    }

    @Override
    public boolean isGuestObject(Object o) {
        return o instanceof JepAccess;
    }

    public static void preInit() {
        // pre-init
        Thread t = new Thread(() -> {
            try (SubInterpreter interp = JEPLanguageDefinition.createSubInterpreter(new File("./"))) {
                interp.exec("print(\"JEP Loaded.\")");
            } catch(Exception e) {
                e.printStackTrace();
            }
        });

        t.start();
    }

    public static void addSharedLibrary(String path) {
        Path p = Core.getInstance().config.configFolder.toPath().getParent().getParent().resolve(path);
        if (!Files.exists(p)) {
            System.err.println("JEP: Shared library path does not exist: " + p);
            return;
        }
        try {
            MainInterpreter.setJepLibraryPath(p.toAbsolutePath().toString());
        } catch (Throwable e) {
            try {
                Field instance = MainInterpreter.class.getDeclaredField("instance");
                instance.setAccessible(true);
                ((MainInterpreter) instance.get(null)).close();
                instance.set(null, null);
                MainInterpreter.setJepLibraryPath(p.toAbsolutePath().toString());
                preInit();
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
