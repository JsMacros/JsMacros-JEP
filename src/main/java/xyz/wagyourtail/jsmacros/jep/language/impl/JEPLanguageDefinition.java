package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.JepConfig;
import jep.SubInterpreter;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.extensions.Extension;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.core.library.BaseLibrary;
import xyz.wagyourtail.jsmacros.jep.config.JEPConfig;

import java.io.File;
import java.util.Map;

public class JEPLanguageDefinition extends BaseLanguage<SubInterpreter, JEPScriptContext> {
    public JEPLanguageDefinition(Extension extension, Core runner) {
        super(extension, runner);
    }

    public static SubInterpreter createSubInterpreter(File folder) {
        return new JepConfig().addSharedModules(Core.getInstance().config.getOptions(JEPConfig.class).sharedLibs.split("[\\s,]")).addIncludePaths(folder.getAbsolutePath()).redirectStdout(System.out).redirectStdErr(System.err).createSubInterpreter();
    }

    protected void execContext(JEPScriptContext ctx, Executor exec) throws Exception {
            SubInterpreter interp = createSubInterpreter(ctx.getContainedFolder());
            ctx.setContext(interp);
            
            for (Map.Entry<String, BaseLibrary> lib : retrieveLibs(ctx).entrySet()) interp.set(lib.getKey(), lib.getValue());
            try {
                exec.accept(interp);
            } finally {
                ctx.tasks.poll();
                EventContainer<?> cc = ctx.getBoundEvents().get(Thread.currentThread());
                if (cc != null) {
                    cc.releaseLock();
                }

                ctx.clearSyncObject();
                if (!ctx.hasMethodWrapperBeenInvoked) {
                    ctx.closeContext();
                }
            }

            VirtualThread joinable;
            while (!ctx.isContextClosed()) {
                joinable = ctx.tasks.peekWaiting(1000);
                if (joinable == null) {
//                    System.out.println("timed out");
                    continue;
                }
//                System.out.println("reEntrantTaskQueuer: joining " + joinable);
                joinable.thread.run();
                ctx.tasks.poll();
            }
    }
    
    @Override
    protected void exec(EventContainer<JEPScriptContext> ctx, ScriptTrigger macro, BaseEvent event) throws Exception {
        execContext(ctx.getCtx(), (interp) -> {
            interp.set("event", event);
            interp.set("file", ctx.getCtx().getFile());
            interp.set("context", ctx);

            interp.runScript(ctx.getCtx().getFile().getCanonicalPath());
        });
    }

    @Override
    protected void exec(EventContainer<JEPScriptContext> ctx, String lang, String script, BaseEvent event) throws Exception {
        execContext(ctx.getCtx(), (interp) -> {
            interp.set("event", event);
            interp.set("file", ctx.getCtx().getFile());
            interp.set("context", ctx);

            interp.exec(script);
        });
    }
    
    @Override
    public JEPScriptContext createContext(BaseEvent event, File file) {
        return new JEPScriptContext(event, file);
    }
    
    protected interface Executor {
        void accept(SubInterpreter interpreter) throws Exception;
    }
}
