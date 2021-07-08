package xyz.wagyourtail.jsmacros.jep.library.impl;

import jep.JepException;
import jep.SharedInterpreter;
import jep.python.PyCallable;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.ContextContainer;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPScriptContext;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Library(value = "JavaWrapper", languages = JEPLanguageDefinition.class)
public class FWrapper extends PerExecLanguageLibrary<SharedInterpreter> implements IFWrapper<PyCallable> {
    
    public FWrapper(ContextContainer<SharedInterpreter> context, Class<? extends BaseLanguage<SharedInterpreter>> language) {
        super(context, language);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> methodToJava(PyCallable c) {
        ((JEPScriptContext) ctx.getCtx()).nonGCdMethodWrappers.incrementAndGet();
        return new JEPMethodWrapper<>(c, true);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> methodToJavaAsync(PyCallable c) {
        return new JEPMethodWrapper<>(c, false);
    }
    
    @Override
    public void stop() {
        ctx.getCtx().closeContext();
    }

    public void deferCurrentTask() throws InterruptedException {
        AtomicBoolean lock = new AtomicBoolean(true);

        ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> lock.set(false));

        while (lock.get()) {
            ((JEPScriptContext)ctx.getCtx()).taskQueue.poll().run();
        }
    }

    private class JEPMethodWrapper<T, U, R> extends MethodWrapper<T, U, R> {
        private final PyCallable fn;
        private final boolean await;

        private JEPMethodWrapper(PyCallable fn, boolean await) {
            this.fn = fn;
            this.await = await;
        }

        private void inner_accept(RunnableEx accepted, boolean await) throws InterruptedException {
            Throwable[] error = {null};
            Semaphore lock = new Semaphore(0);

            // if in the same lua context and not async...
            if (await && ctx.getCtx().getMainThread().get() == Thread.currentThread()) {
                try {
                    accepted.run();
                } catch (JepException e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                try {
                    accepted.run();
                } catch (JepException e) {
                    error[0] = e;
                }
                lock.release();
            });

            if (await) {
                try {
                    lock.acquire();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if (error[0] != null) throw new RuntimeException(error[0]);
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

        @Override
        protected void finalize() throws Throwable {
            int val = ((JEPScriptContext) ctx.getCtx()).nonGCdMethodWrappers.decrementAndGet();
            if (val == 0) ctx.getCtx().closeContext();
        }

    }

    interface RunnableEx {
        void run() throws JepException;
    }
}
