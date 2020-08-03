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
                try {
                    runQueue.put(() -> {
                        c.accept(arg0);
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
    
    public BiConsumer<Object, Object> toBiConsumer(BiConsumer<Object, Object> c) {
        BiConsumer<Object, Object> r = new BiConsumer<Object, Object>() {
            @Override
            public void accept(Object arg0, Object arg1) {
                try {
                    runQueue.put(() -> {
                        c.accept(arg0, arg1);
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

}
