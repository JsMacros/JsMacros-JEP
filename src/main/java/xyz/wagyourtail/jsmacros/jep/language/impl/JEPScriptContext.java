package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.SharedInterpreter;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.ScriptContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class JEPScriptContext extends ScriptContext<SharedInterpreter> {
    public boolean closed = false;
    public final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    public final AtomicInteger nonGCdMethodWrappers = new AtomicInteger(0);

    public JEPScriptContext(BaseEvent event) {
        super(event);
    }

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
