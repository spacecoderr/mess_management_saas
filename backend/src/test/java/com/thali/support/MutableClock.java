package com.thali.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** A clock tests can move around: set it to a campus-local time, then assert cutoff behaviour exactly at the boundary. */
public class MutableClock extends Clock {

    public static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");

    private Instant now;
    private final ZoneId zone;

    public MutableClock() {
        this(CAMPUS);
    }

    private MutableClock(ZoneId zone) {
        this.zone = zone;
        setLocal("2026-11-09T10:30:00");
    }

    public MutableClock setLocal(String isoLocalDateTime) {
        this.now = LocalDateTime.parse(isoLocalDateTime).atZone(zone).toInstant();
        return this;
    }

    public MutableClock set(Instant instant) {
        this.now = instant;
        return this;
    }

    @Override public ZoneId getZone() { return zone; }
    @Override public Clock withZone(ZoneId z) { return new MutableClock(z).set(now); }
    @Override public Instant instant() { return now; }
}
