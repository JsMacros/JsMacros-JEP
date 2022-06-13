package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.JepConfig;
import jep.JepException;
import jep.SubInterpreter;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.jep.config.JEPConfig;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class JEPLanguageDefinition extends BaseLanguage<SubInterpreter> {
    public JEPLanguageDefinition(String extension, Core runner) {
        super(extension, runner);
    }

    public static SubInterpreter createSubInterpreter(File folder) {
        return new JepConfig().addSharedModules(Core.getInstance().config.getOptions(JEPConfig.class).sharedLibs.split("[\\s,]")).addIncludePaths(folder.getAbsolutePath()).redirectStdout(System.out).redirectStdErr(System.err).createSubInterpreter();
    }

    protected void execContext(BaseScriptContext<SubInterpreter> ctx, Executor exec) throws Exception {
        BlockingQueue<Runnable> taskQueue = ((JEPScriptContext) ctx).taskQueue;
        try (SubInterpreter interp = createSubInterpreter(ctx.getContainedFolder())) {
            ctx.setContext(interp);
            
            for (Map.Entry<String, BaseLibrary> lib : retrieveLibs(ctx).entrySet()) interp.set(lib.getKey(), lib.getValue());
        
            exec.accept(interp);
            ctx.releaseBoundEventIfPresent(Thread.currentThread());

            try {
                //clear the sync object earlier since we're still using the thread for the {@link Runnable}s
                ctx.clearSyncObject();
                while (!ctx.isContextClosed()) {
                    Runnable toRun = taskQueue.poll(5000, TimeUnit.MILLISECONDS);
                    if (toRun != null) toRun.run();
                }
            } catch (InterruptedException | NullPointerException ignored) {
            } finally {
                ctx.closeContext();
                interp.close();
            }
        }
    }
    
    @Override
    protected void exec(EventContainer<SubInterpreter> ctx, ScriptTrigger macro, BaseEvent event) throws Exception {
        execContext(ctx.getCtx(), (interp) -> {
            interp.set("event", event);
            interp.set("file", ctx.getCtx().getFile());
            interp.set("context", ctx);

            interp.runScript(ctx.getCtx().getFile().getCanonicalPath());
        });
    }

    @Override
    protected void exec(EventContainer<SubInterpreter> ctx, String script, BaseEvent event) throws Exception {
        execContext(ctx.getCtx(), (interp) -> {
            interp.set("event", event);
            interp.set("file", ctx.getCtx().getFile());
            interp.set("context", ctx);

            interp.exec(script);
        });
    }
    
    @Override
    public BaseScriptContext<SubInterpreter> createContext(BaseEvent event, File file) {
        return new JEPScriptContext(event, file);
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
        void accept(SubInterpreter interpreter) throws Exception;
    }
}
