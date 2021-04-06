package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.JepException;
import jep.SharedInterpreter;
import xyz.wagyourtail.jsmacros.core.language.ScriptContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class JEPScriptContext extends ScriptContext<SharedInterpreter> {
    public boolean closed = false;
    public boolean doLoop = false;
    public final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    
    @Override
    public void closeContext() {
        if (this.context != null) {
            SharedInterpreter ctx = context.get();
            if (ctx != null) {
                try {
                    ctx.close();
                    closed = true;
                    taskQueue.put(() -> {});
                } catch (JepException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
