package xyz.wagyourtail.jsmacros.jep.client;

import jep.MainInterpreter;
import jep.SubInterpreter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.jep.config.JEPConfig;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.jep.library.impl.FWrapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsMacrosJEP implements ModInitializer {
    
    @Override
    public void onInitialize() {
    
        try {
            JsMacros.core.config.addOptions("jep", JEPConfig.class);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }

        addSharedLibrary(JsMacros.core.config.getOptions(JEPConfig.class).path);
    
        JsMacros.core.addLanguage(new JEPLanguageDefinition(".py", JsMacros.core));
        JsMacros.core.libraryRegistry.addLibrary(FWrapper.class);
        
        preInit();
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
        Path p = FabricLoader.getInstance().getGameDir().resolve(path);
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
