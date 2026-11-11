package org.example.dao;

import lombok.Getter;
import lombok.AccessLevel;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class TpsEntity {
    private final AtomicLong lastCheckTime = new AtomicLong(0L);
    @Getter(AccessLevel.NONE)
    private final AtomicLong aCountSent = new AtomicLong(0L);

    public AtomicLong getaCountSent() {
        return aCountSent;
    }
}
