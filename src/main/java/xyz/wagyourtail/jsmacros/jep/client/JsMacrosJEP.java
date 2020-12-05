package xyz.wagyourtail.jsmacros.jep.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jep.JepConfig;
import jep.JepException;
import jep.SharedInterpreter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.jep.library.impl.FConsumerJEP;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JsMacrosJEP implements ClientModInitializer {
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public File configFolder = new File(FabricLoader.getInstance().getConfigDirectory(), "jsMacros");
    public File configFile = new File(configFolder, "jep-options.json");
    
    @Override
    public void onInitializeClient() {
        try {
            JepConfig c = new JepConfig();
            c.setRedirectOutputStreams(true);
            SharedInterpreter.setConfig(c);
        } catch (JepException e) {}
        
        
        Options options = new Options("./jep.dll");
        try {
            options = gson.fromJson(new FileReader(configFile), Options.class);
        } catch (Exception e) {
            try (FileWriter fw = new FileWriter(configFile)) {
                fw.write(gson.toJson(options));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        
        if (options.JEPSharedLibraryPath == null) options.JEPSharedLibraryPath = "./jep.dll";
        
        File f = new File(FabricLoader.getInstance().getGameDirectory(), options.JEPSharedLibraryPath);
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
    
        JsMacros.core.addLanguage(new JEPLanguageDefinition(".py", JsMacros.core));
        JsMacros.core.sortLanguages();
        JsMacros.core.libraryRegistry.addLibrary(FConsumerJEP.class);
        
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

    public static class Options {
        public String JEPSharedLibraryPath;
        public Options(String JEPSharedLibraryPath) {
            this.JEPSharedLibraryPath = JEPSharedLibraryPath;
        }
    }
}
