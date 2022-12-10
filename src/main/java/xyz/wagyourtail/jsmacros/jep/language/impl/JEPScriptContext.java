package xyz.wagyourtail.jsmacros.jep.language.impl;

import jep.SubInterpreter;
import xyz.wagyourtail.PrioryFiFoTaskQueue;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;

import java.io.File;
import java.util.Stack;

public class JEPScriptContext extends BaseScriptContext<SubInterpreter> {
    public final Stack<Thread> overrideThreadStack = new Stack<>();
    public final PrioryFiFoTaskQueue<VirtualThread> tasks = new PrioryFiFoTaskQueue<>(JEPScriptContext::getThreadPriority);
    public JEPScriptContext(BaseEvent event, File file) {
        super(event, file);
        tasks.add(new VirtualThread(null, 5));
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }

    public static int getThreadPriority(Object thread) {
        return -((VirtualThread) thread).priority;
    }

    @Override
    public void wrapSleep(SleepRunnable sleep) throws InterruptedException {
        wrapSleep(0, sleep);
    }

    public void wrapSleep(int changePriority, SleepRunnable sleep) throws InterruptedException {
        assert tasks.peek() != null;
        // remove self from queue
        VirtualThread self = tasks.poll();
        self.priority += changePriority;
        new Thread(() -> {
            try {
                sleep.run();
                // put self at back of the queue
                tasks.add(self);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        reEntrantTaskRunner(self);
    }

    public void reEntrantTaskRunner(VirtualThread endWith) throws InterruptedException {
        VirtualThread joinable;
        while (!isContextClosed()) {
            joinable = tasks.peekWaiting(1000);
            if (joinable == null) {
//                System.out.println("timed out");
                continue;
            }
            if (joinable == endWith) {
//                System.out.println("reEntrantTaskQueuer: endWith " + endWith);
                break;
            }
//            System.out.println("reEntrantTaskQueuer: joining " + joinable);
            joinable.thread.run();
            tasks.poll();
        }
    }

    @Override
    public synchronized void closeContext() {
        super.closeContext();
        getContext().close();
    }

    @Override
    protected void finalize() {
        closeContext();
    }

    public void enter(Thread thread) {
        overrideThreadStack.push(thread);
    }

    public void exit() {
        overrideThreadStack.pop();
    }

    @Override
    public synchronized boolean releaseBoundEventIfPresent(Thread thread) {
        if (thread == mainThread) {
            return super.releaseBoundEventIfPresent(overrideThreadStack.peek());
        }
        return super.releaseBoundEventIfPresent(thread);
    }

    @Override
    public synchronized boolean bindThread(Thread t) {
        if (t == mainThread) {
            return super.bindThread(overrideThreadStack.isEmpty() ? t : overrideThreadStack.peek());
        }
        return super.bindThread(t);
    }

    @Override
    public synchronized void bindEvent(Thread th, EventContainer<BaseScriptContext<SubInterpreter>> event) {
        if (th == mainThread) {
            super.bindEvent(overrideThreadStack.peek(), event);
        } else {
            super.bindEvent(th, event);
        }
    }

    @Override
    public synchronized void unbindThread(Thread t) {
        if (t == mainThread) {
            super.unbindThread(overrideThreadStack.peek());
        } else {
            super.unbindThread(t);
        }
    }

    @Override
    public void setMainThread(Thread t) {
        super.setMainThread(t);
        overrideThreadStack.push(t);
    }

}
