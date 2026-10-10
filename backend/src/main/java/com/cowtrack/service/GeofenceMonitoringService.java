package com.cowtrack.service;

import com.cowtrack.entity.Cow;
import com.cowtrack.entity.LocationRecord;

/**
 * Checks positions against the fences that apply to an animal and raises or
 * closes breach alerts as it crosses them.
 */
public interface GeofenceMonitoringService {

    /**
     * Evaluates one position against the animal's camp and every restricted
     * zone on its farm. A position older than the last one evaluated against a
     * fence is ignored for that fence: a late upload must not reopen a crossing
     * the animal made and came back from days ago.
     *
     * <p>The farm is passed explicitly so this works from collar ingestion as
     * well as from a request.
     */
    void evaluate(Long farmId, Cow cow, LocationRecord fix);
}
