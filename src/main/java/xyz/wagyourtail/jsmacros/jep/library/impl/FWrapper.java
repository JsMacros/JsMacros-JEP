package xyz.wagyourtail.jsmacros.jep.library.impl;

import jep.JepException;
import jep.SharedInterpreter;
import jep.python.PyCallable;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPScriptContext;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Library(value = "JavaWrapper", languages = JEPLanguageDefinition.class)
public class FWrapper extends PerExecLanguageLibrary<SharedInterpreter> implements IFWrapper<PyCallable> {
    
    public FWrapper(BaseScriptContext<SharedInterpreter> context, Class<? extends BaseLanguage<SharedInterpreter>> language) {
        super(context, language);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJava(PyCallable c) {
        return new JEPMethodWrapper<>(ctx, c, true);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(PyCallable c) {
        return new JEPMethodWrapper<>(ctx, c, false);
    }
    
    @Override
    public void stop() {
        ctx.closeContext();
    }

    public void deferCurrentTask() throws InterruptedException {
        AtomicBoolean lock = new AtomicBoolean(true);

        ((JEPScriptContext)ctx).taskQueue.put(() -> lock.set(false));

        while (lock.get()) {
            ((JEPScriptContext)ctx).taskQueue.poll().run();
        }
    }

    private static class JEPMethodWrapper<T, U, R> extends MethodWrapper<T, U, R, BaseScriptContext<SharedInterpreter>> {
        private final PyCallable fn;
        private final boolean await;
        private final Thread overrideThread;

        private JEPMethodWrapper(BaseScriptContext<SharedInterpreter> ctx, PyCallable fn, boolean await) {
            super(ctx);
            this.fn = fn;
            this.await = await;
            this.overrideThread = ctx.getMainThread();
        }

        @Override
        public Thread overrideThread() {
            return overrideThread;
        }

        private void inner_accept(RunnableEx accepted, boolean await) throws InterruptedException {

            // if in the same JEP context and not async...
            if (await) {
                if (ctx.getMainThread() == Thread.currentThread()) {
                    try {
                        accepted.run();
                    } catch (JepException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                ctx.bindThread(Thread.currentThread());
            }

            Throwable[] error = {null};
            Semaphore lock = new Semaphore(0);

            Thread callingThread = Thread.currentThread();
            boolean joinedThread = Core.instance.profile.checkJoinedThreadStack();

            ((JEPScriptContext)ctx).taskQueue.put(() -> {
                try {
                    if (joinedThread) {
                        Core.instance.profile.joinedThreadStack.add(overrideThread);
                    }
                    accepted.run();
                } catch (JepException e) {
                    error[0] = e;
                } finally {
                    Core.instance.profile.joinedThreadStack.remove(overrideThread);

                    ctx.releaseBoundEventIfPresent(overrideThread);

                    lock.release();
                }
            });

            if (await) {
                try {
                    lock.acquire();
                    if (error[0] != null) throw new RuntimeException(error[0]);
                } finally {
                    ctx.unbindThread(Thread.currentThread());
                }
            }
        }

        @Override
        public void accept(T t) {
            try {
                inner_accept(() -> fn.call(t), await);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void accept(T t, U u) {
            try {
                inner_accept(() -> fn.call(t, u), await);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public R apply(T t) {
            Object[] retval = {null};
            try {
                inner_accept(() -> retval[0] = fn.call(t), true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return (R) retval[0];
        }

        @Override
        public R apply(T t, U u) {
            Object[] retval = {null};
            try {
                inner_accept(() -> retval[0] = fn.call(t, u), true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return (R) retval[0];
        }

        @Override
        public boolean test(T t) {
            boolean[] retval = {false};
            try {
                inner_accept(() -> retval[0] = (Boolean) fn.call(t), true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return retval[0];
        }

        @Override
        public boolean test(T t, U u) {
            boolean[] retval = {false};
            try {
                inner_accept(() -> retval[0] = (Boolean) fn.call(t), true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return retval[0];
        }

        @Override
        public void run() {
            try {
                inner_accept(fn::call, await);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int compare(T o1, T o2) {
            int[] retval = {0};
            try {
                inner_accept(() -> retval[0] = (Integer) fn.call(o1, o2), true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return retval[0];
        }

        @Override
        public R get() {
            Object[] retval = {0};
            try {
                inner_accept(() -> retval[0] = fn.call(), true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return (R) retval[0];
        }
    }

    interface RunnableEx {
        void run() throws JepException;
    }
}
