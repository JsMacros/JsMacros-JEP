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
            order.append(i)
            event.putString("test", json.dumps(order))
        JavaWrapper.methodToJavaAsync(5, lambda: add(1)).run()
        JavaWrapper.methodToJavaAsync(5, lambda: add(2)).run()
        JavaWrapper.methodToJavaAsync(6, lambda: add(3)).run()
        add(0)
        JavaWrapper.deferCurrentTask(-2)
        """;
    
    @Test
    public void test() throws InterruptedException {
        Core<ProfileStub, EventRegistryStub> core = CoreInstanceCreator.createCore();
        EventCustom event = new EventCustom("test");
        EventContainer<?> ev = core.exec("py", TEST_SCRIPT, null, event, null, null);
        ev.awaitLock(() -> {});
        assertEquals("[0, 3, 1, 2]", event.getString("test"));
    }

}
