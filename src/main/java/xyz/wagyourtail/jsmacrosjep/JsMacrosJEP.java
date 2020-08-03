package xyz.wagyourtail.jsmacrosjep;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jep.JepConfig;
import jep.JepException;
import jep.SharedInterpreter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import xyz.wagyourtail.jsmacros.config.RawMacro;
import xyz.wagyourtail.jsmacros.runscript.RunScript;
import xyz.wagyourtail.jsmacros.runscript.RunScript.Language;
import xyz.wagyourtail.jsmacros.runscript.functions.Functions;
import xyz.wagyourtail.jsmacrosjep.functions.consumerFunctions;

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
        
        try {
            Class<?> c = Class.forName("xyz.wagyourtail.jsmacrosjython.JsMacrosJython");
            c.getField("hasJEP").set(c, true);
        } catch (Exception e) {}
        
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
        
        for (Functions fun : RunScript.standardLib) {
            if (fun.libName.equals("fs") || fun.libName.equals("time")) {
                fun.excludeLanguages.add(".py");
            }
        }
        
        // register language
        RunScript.addLanguage(new Language() {
            @Override
            public void exec(RawMacro macro, File file, String event, Map<String, Object> args) throws Exception {
                try (SharedInterpreter interp = new SharedInterpreter()) {
                    LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
                    List<Object> remainingTasks = new ArrayList<>();
                    
                    interp.set("event", (Object) event);
                    interp.set("args", args);
                    interp.set("file", file);
                    
                    for (Functions f : RunScript.standardLib) {
                        if (!f.excludeLanguages.contains(".py")) {
                            interp.set(f.libName, f);
                        }
                    }

                    interp.set("consumer", new consumerFunctions("consumer", taskQueue, remainingTasks));
                    
                    interp.exec("import os\nos.chdir('"
                        + file.getParentFile().getCanonicalPath().replaceAll("\\\\", "/") + "')");
                    interp.runScript(file.getCanonicalPath());
                    try {
                        while (remainingTasks.size() > 0) {
                            taskQueue.take().run();
                        }
                    } catch (InterruptedException e) {}
                } catch(Exception e) {
                    throw e;
                }
            }

            @Override
            public String extension() {
                return ".py";
            }
        });
        RunScript.sortLanguages();
        
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
