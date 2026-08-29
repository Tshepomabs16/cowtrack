package com.cowtrack.service;

/**
 * Detects collars that have stopped reporting.
 *
 * <p>This cannot be driven by an incoming position, which is how it was
 * originally attempted: the arrival of a location is proof that the collar is
 * working, so a check triggered by one can never observe silence. Absence has to
 * be looked for on a timer.
 */
public interface CollarMonitoringService {

    /**
     * Sweeps every farm for animals whose most recent position is older than the
     * configured threshold and raises one alert for each newly silent collar.
     *
     * <p>Safe to call repeatedly: an animal with an open no-signal alert is
     * skipped rather than alerted again.
     *
     * @return how many alerts were raised by this pass
     */
    int sweepForSilentCollars();
}
