package xyz.wagyourtail.jsmacros.jep.client;

import jep.JepConfig;
import jep.JepException;
import jep.SharedInterpreter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.jep.config.JEPConfig;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.jep.library.impl.FWrapper;

import java.io.File;
import java.io.IOException;

public class JsMacrosJEP implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        try {
            JepConfig c = new JepConfig();
            c.redirectStdout(System.out);
            c.redirectStdErr(System.err);
            SharedInterpreter.setConfig(c);
        } catch (JepException e) {
            e.printStackTrace();
        }
    
        try {
            JsMacros.core.config.addOptions("jep", JEPConfig.class);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
        addSharedLibrary(JsMacros.core.config.getOptions(JEPConfig.class).path);
    
        JsMacros.core.addLanguage(new JEPLanguageDefinition(".py", JsMacros.core));
        JsMacros.core.libraryRegistry.addLibrary(FWrapper.class);
        
        // pre-init
        Thread t = new Thread(() -> {
            try (SharedInterpreter interp = new SharedInterpreter()) {
                interp.exec("print(\"JEP Loaded.\")");
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
        
        t.start();
    }
    
    public static void addSharedLibrary(String path) {
        File f = new File(FabricLoader.getInstance().getGameDirectory(), path);
        if (f.exists()) {
            File fo = new File(System.getProperty("java.library.path"), f.getName());
            if (!fo.exists()) {
                try {
                    FileUtils.copyFile(f, fo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
