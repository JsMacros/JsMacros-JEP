package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.SubInterpreter;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class JEPScriptContext extends BaseScriptContext<SubInterpreter> {
    public final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();

    public JEPScriptContext(BaseEvent event, File file) {
        super(event, file);
    }
}
