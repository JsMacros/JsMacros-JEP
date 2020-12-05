package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.SharedInterpreter;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class JEPLanguageDefinition extends BaseLanguage {
    public static final Map<Thread, LinkedBlockingQueue<Runnable>> taskQueue = new HashMap<>();
    public static final Map<Thread, Boolean> stopped = new HashMap<>();
    
    public JEPLanguageDefinition(String extension, Core runner) {
        super(extension, runner);
    }
    
    protected void execContext(Executor exec) throws Exception {
        try (SharedInterpreter interp = new SharedInterpreter()) {
            stopped.put(Thread.currentThread(), true);
            taskQueue.put(Thread.currentThread(), new LinkedBlockingQueue<>());
        
            for (Map.Entry<String, BaseLibrary> lib : retrieveLibs(interp).entrySet()) interp.set(lib.getKey(), lib.getValue());
        
            exec.accept(interp);
        
            try {
                while (!stopped.get(Thread.currentThread())) {
                    taskQueue.get(Thread.currentThread()).take().run();
                }
            } catch (InterruptedException e) {}
        } catch(Exception e) {
            throw e;
        } finally {
            taskQueue.remove(Thread.currentThread());
            stopped.remove(Thread.currentThread());
        }
    }
    
    @Override
    public void exec(ScriptTrigger macro, File file, BaseEvent event) throws Exception {
        execContext((interp) -> {
            interp.set("event", event);
            interp.set("file", file);
            
            interp.exec("import os\nos.chdir('"
                + file.getParentFile().getCanonicalPath().replaceAll("\\\\", "/") + "')");
            interp.runScript(file.getCanonicalPath());
        });
    }
    
    @Override
    public void exec(String script, Map<String, Object> globals, Path path) throws Exception {
        execContext((interp) -> {
            if (globals != null) for (Map.Entry<String, Object> e : globals.entrySet()) {
                interp.set(e.getKey(), e.getValue());
            }
    
            interp.exec(script);
        });
    }
    
    protected interface Executor {
        void accept(SharedInterpreter interpreter) throws Exception;
    }
}
