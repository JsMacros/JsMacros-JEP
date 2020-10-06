package xyz.wagyourtail.jsmacrosjep.functions;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import jep.python.PyCallable;
import xyz.wagyourtail.jsmacros.extensionbase.Functions;
import xyz.wagyourtail.jsmacros.extensionbase.IFConsumer;
import xyz.wagyourtail.jsmacros.extensionbase.MethodWrapper;

public class FConsumerJEP extends Functions implements IFConsumer<PyCallable, PyCallable, PyCallable> {

    private LinkedBlockingQueue<Runnable> runQueue;
    private List<Object> preRun;
    
    public FConsumerJEP(String libName) {
        super(libName);
        // TODO Auto-generated constructor stub
    }

    public FConsumerJEP(String libName,  LinkedBlockingQueue<Runnable> runQueue,  List<Object> preRunQueue) {
        super(libName);
        this.preRun = preRunQueue;
        this.runQueue = runQueue;
    }
    

    @Override
    public MethodWrapper<Object, Object> autoWrap(PyCallable c) {
            MethodWrapper<Object, Object> r = new MethodWrapper<Object, Object>() {
            
            @Override
            public void accept(Object arg0, Object arg1) {
                synchronizer s = new synchronizer();
                try {
                    runQueue.put(() -> {
                        try {
                            c.call(arg0, arg1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                preRun.remove(this);
            }

            @Override
            public void accept(Object arg0) {
                synchronizer s = new synchronizer();
                try {
                    runQueue.put(() -> {
                        try {
                            c.call(arg0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        s.gainOwnershipAndNotifyAll();
                    });
                    s.gainOwnershipAndWait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                preRun.remove(this);
            }
        };
        preRun.add(r);
        return r;
    }

    @Override
    public MethodWrapper<Object, Object> autoWrapAsync(PyCallable c) {
            MethodWrapper<Object, Object> r = new MethodWrapper<Object, Object>() {
            
            @Override
            public void accept(Object arg0, Object arg1) {
                try {
                    runQueue.put(() -> {
                        try {
                            c.call(arg0, arg1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                preRun.remove(this);
            }

            @Override
            public void accept(Object arg0) {
                try {
                    runQueue.put(() -> {
                        try {
                            c.call(arg0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                preRun.remove(this);
            }
        };
        preRun.add(r);
        return r;
    }

    @Override
    public MethodWrapper<Object, Object> toConsumer(PyCallable c) {
        return autoWrap(c);
    }

    @Override
    public MethodWrapper<Object, Object> toBiConsumer(PyCallable c) {
        return autoWrap(c);
    }

    @Override
    public MethodWrapper<Object, Object> toAsyncConsumer(PyCallable c) {
       return autoWrapAsync(c);
    }

    @Override
    public MethodWrapper<Object, Object> toAsyncBiConsumer(PyCallable c) {
        return autoWrapAsync(c);
    }
    
    public void stop() {
        Thread.currentThread().interrupt();
    }
    
    public static class synchronizer {
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
