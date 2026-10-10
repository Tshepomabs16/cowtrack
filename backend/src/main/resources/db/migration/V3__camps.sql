-- V3: Geofences become farm-level camps and restricted zones.
--
-- Until now a geofence was a circle belonging to exactly one cow. Farms are not
-- laid out like that: a camp (paddock) is a piece of land that a group of
-- animals is put into, and the alert that matters is an animal leaving it. A
-- herd of 200 needed 200 identical circles, and nothing could describe a dam or
-- a neighbour's crop that the herd must stay out of.
--
-- After this migration:
--   * a geofence belongs to the farm, and is a circle or a polygon;
--   * KEEP_IN fences are camps: each animal is in at most one (cows.camp_id),
--     and leaving it raises the alert;
--   * KEEP_OUT fences are restricted zones that apply to the whole herd;
--   * a fence can be switched off (grazing rotation) and retired rather than
--     deleted once it has raised alerts, so a theft case keeps its evidence.

ALTER TABLE geofences ADD COLUMN name VARCHAR(120);
ALTER TABLE geofences ADD COLUMN description VARCHAR(500);
ALTER TABLE geofences ADD COLUMN fence_type VARCHAR(16) NOT NULL DEFAULT 'KEEP_IN';
ALTER TABLE geofences ADD COLUMN shape VARCHAR(16) NOT NULL DEFAULT 'CIRCLE';
-- [[lat,lng],...] for polygons; NULL for circles.
ALTER TABLE geofences ADD COLUMN vertices_json VARCHAR(20000);
ALTER TABLE geofences ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE geofences ADD COLUMN retired_at TIMESTAMP;
ALTER TABLE geofences ADD COLUMN updated_at TIMESTAMP;

-- A polygon has no centre or radius.
ALTER TABLE geofences ALTER COLUMN center_latitude DROP NOT NULL;
ALTER TABLE geofences ALTER COLUMN center_longitude DROP NOT NULL;
ALTER TABLE geofences ALTER COLUMN radius_meters DROP NOT NULL;

-- Each animal's existing circle becomes its camp, so nobody loses the
-- monitoring they had. Identical circles are left as separate camps rather
-- than merged: merging is a judgement the farmer should make, not a migration.
ALTER TABLE cows ADD COLUMN camp_id BIGINT;
ALTER TABLE cows ADD CONSTRAINT fk_cows_camp FOREIGN KEY (camp_id) REFERENCES geofences (geofence_id);

UPDATE cows SET camp_id = (SELECT g.geofence_id FROM geofences g WHERE g.cow_id = cows.cow_id);

UPDATE geofences SET name = (SELECT 'Camp for ' || c.tag_id FROM cows c WHERE c.cow_id = geofences.cow_id);
UPDATE geofences SET name = 'Camp ' || geofence_id WHERE name IS NULL;
ALTER TABLE geofences ALTER COLUMN name SET NOT NULL;

ALTER TABLE geofences DROP CONSTRAINT uk_geofences_cow_id;
ALTER TABLE geofences DROP CONSTRAINT fk_geofences_cow;
ALTER TABLE geofences DROP COLUMN cow_id;

CREATE INDEX idx_geofences_farm_active ON geofences (farm_id, is_active);
CREATE INDEX idx_cows_camp ON cows (camp_id);

-- Where each animal stands relative to each fence that applies to it, so an
-- alert is raised when that changes rather than on every position. The pending
-- count is how many fixes in a row have disagreed with the confirmed side:
-- collar GPS wanders, and cattle graze right along fence lines.
CREATE TABLE geofence_occupancy (
    geofence_id   BIGINT NOT NULL,
    cow_id        BIGINT NOT NULL,
    is_inside     BOOLEAN NOT NULL,
    pending_fixes INTEGER NOT NULL DEFAULT 0,
    last_fix_at   TIMESTAMP NOT NULL,
    CONSTRAINT pk_geofence_occupancy PRIMARY KEY (geofence_id, cow_id),
    CONSTRAINT fk_occupancy_geofence FOREIGN KEY (geofence_id) REFERENCES geofences (geofence_id),
    CONSTRAINT fk_occupancy_cow FOREIGN KEY (cow_id) REFERENCES cows (cow_id)
);

-- A breach alert names the fence that raised it, so one alert can be kept open
-- per animal per fence and closed automatically when the animal comes back.
ALTER TABLE alerts ADD COLUMN geofence_id BIGINT;
ALTER TABLE alerts ADD CONSTRAINT fk_alerts_geofence FOREIGN KEY (geofence_id) REFERENCES geofences (geofence_id);
ALTER TABLE alerts ADD COLUMN resolved_at TIMESTAMP;
ALTER TABLE alerts ADD COLUMN resolution_note VARCHAR(255);

CREATE INDEX idx_alerts_geofence ON alerts (geofence_id);
