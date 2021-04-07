package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.JepException;
import jep.SharedInterpreter;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.language.ScriptContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class JEPScriptContext extends ScriptContext<SharedInterpreter> {
    public boolean closed = false;
    public boolean doLoop = false;
    public final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    
    @Override
    public boolean isContextClosed() {
        return super.isContextClosed() || closed;
    }
    
    @Override
    public void closeContext() {
        if (this.context != null) {
            SharedInterpreter ctx = context.get();
            if (ctx != null) {
                closed = true;
                Core.instance.threadContext.entrySet().stream().filter(e -> e.getValue() == this).forEach(e -> e.getKey().interrupt());
            }
        }
    }
    
}
