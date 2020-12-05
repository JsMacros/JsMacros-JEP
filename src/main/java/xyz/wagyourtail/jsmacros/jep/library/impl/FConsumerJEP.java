package xyz.wagyourtail.jsmacros.jep.library.impl;

import jep.python.PyCallable;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.language.BaseLanguage;
import xyz.wagyourtail.jsmacros.jep.language.impl.JEPLanguageDefinition;
import xyz.wagyourtail.jsmacros.core.library.IFConsumer;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary;

import java.util.concurrent.atomic.AtomicReference;

@Library(value = "consumer", languages = JEPLanguageDefinition.class)
public class FConsumerJEP extends PerExecLanguageLibrary<IFConsumer> implements IFConsumer<PyCallable, PyCallable, PyCallable> {
    private boolean first = true;
    
    public FConsumerJEP(Class<? extends BaseLanguage> language, Object context, Thread thread) {
        super(language, context, thread);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> toConsumer(PyCallable c) {
        return autoWrap(c);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> toBiConsumer(PyCallable c) {
        return autoWrap(c);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> toAsyncConsumer(PyCallable c) {
        return autoWrapAsync(c);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> toAsyncBiConsumer(PyCallable c) {
        return autoWrapAsync(c);
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> autoWrap(PyCallable c) {
        if (first) {
            JEPLanguageDefinition.stopped.put(thread, false);
            first = false;
        }
        return new MethodWrapper<A, B, R>() {
    
            @Override
            public R get() {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
                
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            retval.set((R) c.call());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
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
                
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            c.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    
            @Override
            public void accept(A a) {
                Synchronizer s = new Synchronizer();
    
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            c.call(a);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    
            @Override
            public void accept(A a, B b) {
                Synchronizer s = new Synchronizer();
    
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            c.call(a, b);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    
            @Override
            public R apply(A a) {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
    
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            retval.set((R) c.call(a));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
    
                return retval.get();
            }
    
            @Override
            public R apply(A a, B b) {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
    
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            retval.set((R) c.call(a, b));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
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
        };
    }
    
    @Override
    public <A, B, R> MethodWrapper<A, B, R> autoWrapAsync(PyCallable c) {
        if (first) {
            JEPLanguageDefinition.stopped.put(thread, false);
            first = false;
        }
        return new MethodWrapper<A, B, R>() {
        
            @Override
            public R get() {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
            
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            retval.set((R) c.call());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
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
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
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
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
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
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
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
            
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            retval.set((R) c.call(a));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            
                return retval.get();
            }
        
            @Override
            public R apply(A a, B b) {
                Synchronizer s = new Synchronizer();
                AtomicReference<R> retval = new AtomicReference<>();
            
                try {
                    JEPLanguageDefinition.taskQueue.get(thread).put(() -> {
                        try {
                            retval.set((R) c.call(a, b));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
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
        };
    }
    
    @Override
    public void stop() {
        JEPLanguageDefinition.stopped.put(thread, true);
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
