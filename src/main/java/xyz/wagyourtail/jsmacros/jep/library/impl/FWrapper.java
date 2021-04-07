package xyz.wagyourtail.jsmacros.jep.library.impl;

import jep.SharedInterpreter;
import jep.python.PyCallable;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.core.language.ContextContainer;
import xyz.wagyourtail.jsmacros.core.library.IFWrapper;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPScriptContext;

import java.util.concurrent.atomic.AtomicReference;

@Library(value = "JavaWrapper", languages = JEPLanguageDefinition.class)
public class FWrapper extends PerExecLanguageLibrary<SharedInterpreter> implements IFWrapper<PyCallable> {
    private boolean first = true;
    private final Thread t;
    
    public FWrapper(ContextContainer<SharedInterpreter> context, Class<? extends BaseLanguage<SharedInterpreter>> language) {
        super(context, language);
        t = context.getLockThread();
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> methodToJava(PyCallable c) {
        if (first) {
            ((JEPScriptContext)ctx.getCtx()).doLoop = true;
            first = false;
        }
        return new MethodWrapper<A, B, R>() {
    
            @Override
            public R get() {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
                AtomicReference<Throwable> ev = new AtomicReference<>();
                
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            retval.set((R) c.call());
                        } catch (Exception e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                return retval.get();
            }
    
            @Override
            public int compare(A o1, A o2) {
                return (Integer) apply(o1, (B) o2);
            }
    
            @Override
            public void run() {
                Synchronizer s = new Synchronizer();
                AtomicReference<Throwable> ev = new AtomicReference<>();
                
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            c.call();
                        } catch (Exception e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    
            @Override
            public void accept(A a) {
                Synchronizer s = new Synchronizer();
                AtomicReference<Throwable> ev = new AtomicReference<>();
    
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            c.call(a);
                        } catch (Exception e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    
            @Override
            public void accept(A a, B b) {
                Synchronizer s = new Synchronizer();
                AtomicReference<Throwable> ev = new AtomicReference<>();
    
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            c.call(a, b);
                        } catch (Exception e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    
            @Override
            public R apply(A a) {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
                AtomicReference<Throwable> ev = new AtomicReference<>();
    
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            retval.set((R) c.call(a));
                        } catch (Exception e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
    
                return retval.get();
            }
    
            @Override
            public R apply(A a, B b) {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
                AtomicReference<Throwable> ev = new AtomicReference<>();
    
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            retval.set((R) c.call(a, b));
                        } catch (Exception e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
    
                return retval.get();
            }
    
            @Override
            public boolean test(A a) {
                return (Boolean) apply(a);
            }
    
            @Override
            public boolean test(A a, B b) {
                return (Boolean) apply(a, b);
            }
    
            @Override
            public Thread overrideThread() {
                return t;
            }
        };
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> methodToJavaAsync(PyCallable c) {
        if (first) {
            ((JEPScriptContext)ctx.getCtx()).doLoop = true;
            first = false;
        }
        return new MethodWrapper<A, B, R>() {
        
            @Override
            public R get() {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
                AtomicReference<Throwable> ev = new AtomicReference<>();
            
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            retval.set((R) c.call());
                        } catch (Exception e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            
                return retval.get();
            }
        
            @Override
            public int compare(A o1, A o2) {
                return (Integer) apply(o1, (B) o2);
            }
        
            @Override
            public void run() {
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            c.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        
            @Override
            public void accept(A a) {
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            c.call(a);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        
            @Override
            public void accept(A a, B b) {
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            c.call(a, b);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        
            @Override
            public R apply(A a) {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
                AtomicReference<Throwable> ev = new AtomicReference<>();
            
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            retval.set((R) c.call(a));
                        } catch (Exception e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            
                return retval.get();
            }
        
            @Override
            public R apply(A a, B b) {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
                AtomicReference<Throwable> ev = new AtomicReference<>();
                try {
                    if (ctx.getCtx().isContextClosed()) throw new RuntimeException("Context Closed");
                    ((JEPScriptContext)ctx.getCtx()).taskQueue.put(() -> {
                        try {
                            retval.set((R) c.call(a, b));
                        } catch (Throwable e) {
                            ev.set(e);
                        } finally {
                            s.gainOwnershipAndNotifyAll();
                        }
                    });
                    s.gainOwnershipAndWait();
                    if (ev.get() != null) throw new RuntimeException(ev.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            
                return retval.get();
            }
        
            @Override
            public boolean test(A a) {
                return (Boolean) apply(a);
            }
        
            @Override
            public boolean test(A a, B b) {
                return (Boolean) apply(a, b);
            }
            
            @Override
            public Thread overrideThread() {
                return t;
            }
        };
    }
    
    @Override
    public void stop() {
        ctx.getCtx().closeContext();
    }
    
    protected static class Synchronizer {
        private boolean falseFlag = true;
        public synchronized void gainOwnershipAndWait() throws InterruptedException {
            while (falseFlag) this.wait();
        }
        
        public synchronized void gainOwnershipAndNotifyAll() {
            falseFlag = false;
            this.notifyAll();
        }
    }
}
