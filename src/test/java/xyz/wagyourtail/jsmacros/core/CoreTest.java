package xyz.wagyourtail.jsmacros.core;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jsmacros.core.event.impl.EventCustom;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.stubs.CoreInstanceCreator;
import xyz.wagyourtail.jsmacros.stubs.EventRegistryStub;
import xyz.wagyourtail.jsmacros.stubs.ProfileStub;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoreTest {
    @Language("py")
    private final String TEST_SCRIPT = """
        import json
        order = []
        def add(i):
            print(i)
            order.append(i)
        JavaWrapper.methodToJavaAsync(5, lambda: add(1)).run()
        JavaWrapper.methodToJavaAsync(5, lambda: add(2)).run()
        JavaWrapper.methodToJavaAsync(6, lambda: add(3)).run()
        add(0)
        JavaWrapper.deferCurrentTask(-2)
        event.putString("test", json.dumps(order))
        """;

    @Language("py")
    private final String TEST_SCRIPT2 = """
        i = 0
        def tick(a, b):
            global i;
            i += 1
            if i == 10:
                JsMacros.off("tick", tickEv)
                event.putInt("i", i)
        tickEv = JsMacros.on("tick", JavaWrapper.methodToJava(tick))
        tick(1, 2)
        """;

    @Test
    public void test() throws InterruptedException {
        Core<ProfileStub, EventRegistryStub> core = CoreInstanceCreator.createCore();
        EventCustom event = new EventCustom("test");
        EventContainer<?> ev = core.exec("py", TEST_SCRIPT, null, event, null, null);
        ev.awaitLock(() -> {});
        assertEquals("[0, 3, 1, 2]", event.getString("test"));
    }

    @Test
    public void test2() throws InterruptedException {
        Core<ProfileStub, EventRegistryStub> core = CoreInstanceCreator.createCore();
        EventCustom event = new EventCustom("test");
        EventContainer<?> ev = core.exec("py", TEST_SCRIPT2, null, event, null, null);
        Thread.sleep(1000);
        assertEquals(10, event.getInt("i"));
    }

}
