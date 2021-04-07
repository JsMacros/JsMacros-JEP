package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.JepException;
import jep.SharedInterpreter;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.language.ContextContainer;
import xyz.wagyourtail.jsmacros.core.language.ScriptContext;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class JEPLanguageDefinition extends BaseLanguage<SharedInterpreter> {
    public JEPLanguageDefinition(String extension, Core runner) {
        super(extension, runner);
    }
    
    protected void execContext(ContextContainer<SharedInterpreter> ctx, Executor exec) throws Exception {
        BlockingQueue<Runnable> taskQueue = ((JEPScriptContext) ctx.getCtx()).taskQueue;
        try (SharedInterpreter interp = new SharedInterpreter()) {
            ctx.getCtx().setContext(interp);
            
            for (Map.Entry<String, BaseLibrary> lib : retrieveLibs(ctx).entrySet()) interp.set(lib.getKey(), lib.getValue());
        
            exec.accept(interp);
            ctx.releaseLock();
            
            if (!((JEPScriptContext) ctx.getCtx()).doLoop) return;
            try {
                while (!((JEPScriptContext) ctx.getCtx()).closed) {
                    taskQueue.take().run();
                }
            } catch (InterruptedException ignored) {}
        }
        taskQueue.forEach(Runnable::run);
    }
    
    @Override
    protected void exec(ContextContainer<SharedInterpreter> ctx, ScriptTrigger macro, File file, BaseEvent event) throws Exception {
        execContext(ctx, (interp) -> {
            interp.set("event", event);
            interp.set("file", file);
            interp.set("context", ctx);
            
            interp.exec("import os\nos.chdir('"
                + file.getParentFile().getCanonicalPath().replaceAll("\\\\", "/") + "')");
            interp.runScript(file.getCanonicalPath());
        });
    }
    
    @Override
    protected void exec(ContextContainer<SharedInterpreter> ctx, String script, Map<String, Object> globals, Path path) throws Exception {
        execContext(ctx, (interp) -> {
            if (globals != null) for (Map.Entry<String, Object> e : globals.entrySet()) {
                interp.set(e.getKey(), e.getValue());
            }
            interp.set("context", ctx);
    
            interp.exec(script);
        });
    }
    
    @Override
    public ScriptContext<SharedInterpreter> createContext() {
        return new JEPScriptContext();
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
    
    protected interface Executor {
        void accept(SharedInterpreter interpreter) throws Exception;
    }
}
