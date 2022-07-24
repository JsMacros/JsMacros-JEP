package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.Jep;
import jep.SubInterpreter;
import xyz.wagyourtail.PrioryFiFoTaskQueue;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public class JEPScriptContext extends BaseScriptContext<SubInterpreter> {
    public final PrioryFiFoTaskQueue<WrappedThread> tasks = new PrioryFiFoTaskQueue<>(JEPScriptContext::getThreadPriority);
    public JEPScriptContext(BaseEvent event, File file) {
        super(event, file);
        tasks.add(new WrappedThread(Thread.currentThread(), 5));
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }

    public static int getThreadPriority(Object thread) {
        return -((WrappedThread) thread).priority;
    }

    @Override
    public void wrapSleep(SleepRunnable sleep) throws InterruptedException {
        wrapSleep(0, sleep);
    }

    public void wrapSleep(int changePriority, SleepRunnable sleep) throws InterruptedException {
        leave();

        try {
            assert tasks.peek() != null;
            // remove self from queue
            int prio = tasks.poll().release();

            sleep.run();

            // put self at back of the queue
            tasks.add(new WrappedThread(Thread.currentThread(), prio + changePriority));

            // wait to be at the front of the queue again
            WrappedThread joinable = tasks.peek();
            assert joinable != null;
            while (joinable.thread != Thread.currentThread()) {
                joinable.waitFor();
                joinable = tasks.peek();
                assert joinable != null;
            }
        } finally {
            enter();
        }
    }

    private static final Field f;
    static {
        try {
            f = Jep.class.getDeclaredField("thread");
            f.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void enter() {
        try {
            if (f.get(getContext()) != null) {
                throw new IllegalStateException("Thread already set");
            }
            f.set(getContext(), Thread.currentThread());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void leave() {
        try {
            f.set(getContext(), null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void closeContext() {
        super.closeContext();

        // force on right thread to close
        try {
            f.set(getContext(), Thread.currentThread());
            getContext().close();
        } catch (Throwable ignored) {}
    }

    @Override
    protected void finalize() {
        closeContext();
    }

}
