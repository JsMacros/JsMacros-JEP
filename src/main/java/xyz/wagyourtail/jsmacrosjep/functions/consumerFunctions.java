package xyz.wagyourtail.jsmacrosjep.functions;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import xyz.wagyourtail.jsmacros.runscript.functions.Functions;

public class consumerFunctions extends Functions {
    private LinkedBlockingQueue<Runnable> runQueue;
    private List<Object> preRun;
    
    public consumerFunctions(String libName,  LinkedBlockingQueue<Runnable> runQueue,  List<Object> preRunQueue) {
        super(libName);
        this.preRun = preRunQueue;
        this.runQueue = runQueue;
    }
    
    public Consumer<Object> toConsumer(Consumer<Object> c) {
        Consumer<Object> r = new Consumer<Object>() {
            
            @Override
            public void accept(Object arg0) {
                synchronizer s = new synchronizer();
                try {
                    runQueue.put(() -> {
                        try {
                            c.accept(arg0);
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
    
    public BiConsumer<Object, Object> toBiConsumer(BiConsumer<Object, Object> c) {
        BiConsumer<Object, Object> r = new BiConsumer<Object, Object>() {
            
            @Override
            public void accept(Object arg0, Object arg1) {
                synchronizer s = new synchronizer();
                try {
                    runQueue.put(() -> {
                        try {
                            c.accept(arg0, arg1);
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
    
    public Consumer<Object> toAsyncConsumer(Consumer<Object> c) {
        Consumer<Object> r = new Consumer<Object>() {
            
            @Override
            public void accept(Object arg0) {
                try {
                    runQueue.put(() -> {
                        try {
                            c.accept(arg0);
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
    
    public BiConsumer<Object, Object> toAsyncBiConsumer(BiConsumer<Object, Object> c) {
        BiConsumer<Object, Object> r = new BiConsumer<Object, Object>() {
            
            @Override
            public void accept(Object arg0, Object arg1) {
                try {
                    runQueue.put(() -> {
                        try {
                            c.accept(arg0, arg1);
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
