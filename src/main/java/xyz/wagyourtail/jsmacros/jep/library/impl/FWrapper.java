package xyz.wagyourtail.jsmacros.jep.library.impl;

import jep.JepException;
import jep.SubInterpreter;
import jep.python.PyCallable;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPScriptContext;
import xyz.wagyourtail.jsmacros.jep.language.impl.VirtualThread;

import java.util.concurrent.Semaphore;

@Library(value = "JavaWrapper", languages = JEPLanguageDefinition.class)
public class FWrapper extends PerExecLanguageLibrary<SubInterpreter, JEPScriptContext> implements IFWrapper<PyCallable> {
    
    public FWrapper(JEPScriptContext context, Class<JEPLanguageDefinition> language) {
        super(context, language);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJava(PyCallable c) {
        return new JEPMethodWrapper<>(ctx, c, true, 5);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(PyCallable c) {
        return new JEPMethodWrapper<>(ctx, c, false, 5);
    }

    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(int priority, PyCallable c) {
        return new JEPMethodWrapper<>(ctx, c, false, priority);
    }

    @Override
    public void stop() {
        ctx.closeContext();
    }

    @Override
    public void deferCurrentTask() throws InterruptedException {
        ctx.wrapSleep(() -> {});
    }

    @Override
    public void deferCurrentTask(int priorityAdjust) throws InterruptedException {
        ctx.wrapSleep(priorityAdjust, () -> {});
    }

    private static class JEPMethodWrapper<T, U, R> extends MethodWrapper<T, U, R, JEPScriptContext> {
        private final PyCallable fn;
        private final boolean await;
        private final int priority;


        private JEPMethodWrapper(JEPScriptContext ctx, PyCallable fn, boolean await, int priority) {
            super(ctx);
            this.fn = fn;
            this.await = await;
            this.priority = priority;
        }

        private void inner_accept(RunnableEx accepted) throws InterruptedException {

            // if in the same JEP context and not async...
            Semaphore s = new Semaphore(await ? 0 : 1);

            Thread overrideThread = new Thread() {
                @Override
                public void interrupt() {
                    ctx.getMainThread().interrupt();
                }
            };

            Runnable accept = () -> {
                if (ctx.isContextClosed()) {
                    throw new BaseScriptContext.ScriptAssertionError("Context closed");
                }
                try {
                    ctx.overrideThreadStack.push(overrideThread);
                    ctx.bindThread(overrideThread);
                    accepted.run();
                } catch (Throwable ex) {
                    Core.getInstance().profile.logError(ex);
                } finally {
                    ctx.releaseBoundEventIfPresent(overrideThread);
                    Core.getInstance().profile.joinedThreadStack.remove(overrideThread);
                    ctx.unbindThread(overrideThread);

                    ctx.overrideThreadStack.pop();
                    s.release();
                }
            };

//            System.out.println("JEPMethodWrapper.inner_accept: " + priority);
            ctx.tasks.add(new VirtualThread(accept, priority));
            s.acquire();
        }

        private void inner_apply(RunnableEx accepted) {
            Semaphore sem = new Semaphore(0);
            ctx.bindThread(Thread.currentThread());
            try {
                inner_accept(() -> {
                    accepted.run();
                    sem.release();
                });
                sem.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                ctx.releaseBoundEventIfPresent(Thread.currentThread());
                Core.getInstance().profile.joinedThreadStack.remove(Thread.currentThread());
                ctx.unbindThread(Thread.currentThread());
            }
        }

        @Override
        public void accept(T t) {
            try {
                inner_accept(() -> fn.call(t));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void accept(T t, U u) {
            try {
                inner_accept(() -> fn.call(t, u));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public R apply(T t) {
            Object[] retval = {null};
            inner_apply(() -> retval[0] = fn.call(t));
            return (R) retval[0];
        }

        @Override
        public R apply(T t, U u) {
            Object[] retval = {null};
            inner_apply(() -> retval[0] = fn.call(t, u));
            return (R) retval[0];
        }

        @Override
        public boolean test(T t) {
            boolean[] retval = {false};
            inner_apply(() -> retval[0] = (Boolean) fn.call(t));
            return retval[0];
        }

        @Override
        public boolean test(T t, U u) {
            boolean[] retval = {false};
            inner_apply(() -> retval[0] = (Boolean) fn.call(t));
            return retval[0];
        }

        @Override
        public void run() {
            try {
                inner_accept(fn::call);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int compare(T o1, T o2) {
            int[] retval = {0};
            inner_apply(() -> retval[0] = (Integer) fn.call(o1, o2));
            return retval[0];
        }

        @Override
        public R get() {
            Object[] retval = {0};
            inner_apply(() -> retval[0] = fn.call());
            return (R) retval[0];
        }

    }

    interface RunnableEx {
        void run() throws JepException;
    }
}
