package xyz.wagyourtail.jsmacros.jep.language.impl;

public class VirtualThread {
    public final Runnable thread;
    public int priority;

    public VirtualThread(Runnable thread, int priority) {
        this.thread = thread;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "VirtualThread{" +
                "thread=" + thread +
                ", priority=" + priority +
                '}';
    }

}
