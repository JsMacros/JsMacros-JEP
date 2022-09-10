package xyz.wagyourtail.jsmacros.stubs;

import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;

@Event("tick")
public class EventTick implements BaseEvent {

    EventTick() {
        profile.triggerEvent(this);
    }
}
