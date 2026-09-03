package com.cowtrack;

import com.cowtrack.dto.response.LiveCowResponse;
import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.LocationRecord;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.LocationRecordRepository;
import com.cowtrack.security.FarmContext;
import com.cowtrack.service.LocationService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The live map used to run a position lookup per animal. A correctness test
 * cannot see that — the map returned the right cattle either way — so it stayed
 * invisible until a herd was large enough for the page to hang.
 *
 * <p>This asserts the shape of the cost rather than a query count: the same work
 * is done for a herd of three and a herd of thirty. A number would only pin
 * today's implementation, and would need revising every time an unrelated query
 * moved, which is how that kind of test ends up deleted.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class LiveMapQueryCountTest {

    @Autowired private LocationService locationService;
    @Autowired private FarmRepository farmRepository;
    @Autowired private CowRepository cowRepository;
    @Autowired private LocationRecordRepository locationRepository;
    @Autowired private FarmContext farmContext;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    /** A farm of its own, so other tests' data cannot change the counts. */
    private Farm herdOf(int size) {
        Farm farm = new Farm();
        farm.setFarmName("Query Count " + System.nanoTime());
        farm = farmRepository.save(farm);

        for (int i = 0; i < size; i++) {
            Cow cow = new Cow();
            cow.setFarm(farm);
            cow.setTagId("QC-" + System.nanoTime() + "-" + i);
            cow.setName("Cow " + i);
            cow = cowRepository.save(cow);

            // Two positions each, so "the newest one" is a real choice rather
            // than the only row available.
            for (int n = 0; n < 2; n++) {
                LocationRecord record = new LocationRecord();
                record.setFarm(farm);
                record.setCow(cow);
                record.setLatitude(new BigDecimal("-23.90" + i));
                record.setLongitude(new BigDecimal("29.46" + i));
                record.setRecordedAt(LocalDateTime.now().minusMinutes(10L - n));
                locationRepository.save(record);
            }
        }
        return farm;
    }

    private long queriesToLoadLiveMap(Farm farm, int expectedCattle) {
        Statistics statistics = statistics();
        statistics.clear();

        List<LiveCowResponse> live = farmContext.actingAsFarm(
                farm.getFarmId(), locationService::getLiveLocations);

        // Guards the measurement: a query count means nothing if the call did
        // not actually produce the herd.
        assertThat(live).hasSize(expectedCattle);
        return statistics.getPrepareStatementCount();
    }

    @Test
    void loadingTheLiveMapCostsTheSameForALargeHerdAsASmallOne() {
        long forThree = queriesToLoadLiveMap(herdOf(3), 3);
        long forThirty = queriesToLoadLiveMap(herdOf(30), 30);

        assertThat(forThirty)
                .as("ten times the cattle should not mean more queries; "
                        + "%d for three head, %d for thirty", forThree, forThirty)
                .isEqualTo(forThree);
    }

    /**
     * Two queries: the positions with their animals joined in, and the set of
     * animals with an open alert, which is what colours a marker.
     *
     * <p>Pinned to an exact number as well as to the scaling property above,
     * because the two failures look different. Scaling catches a lookup that runs
     * per animal; this catches a second constant query creeping in unnoticed.
     * Notably the name join: the response carries each animal's name, and without
     * the JOIN FETCH Hibernate resolves that eager association with an extra
     * select per animal.
     */
    @Test
    void theLiveMapLoadsInTwoQueries() {
        assertThat(queriesToLoadLiveMap(herdOf(5), 5)).isEqualTo(2);
    }

    @Test
    void theNewestPositionIsTheOneReturned() {
        Farm farm = herdOf(1);

        List<LiveCowResponse> live = farmContext.actingAsFarm(
                farm.getFarmId(), locationService::getLiveLocations);

        LocationRecord newest = locationRepository
                .findLatestByFarmIdAndCowId(farm.getFarmId(), live.get(0).getCowId())
                .orElseThrow();

        assertThat(live).hasSize(1);
        assertThat(live.get(0).getLocationId()).isEqualTo(newest.getLocationId());
        assertThat(live.get(0).getCowName()).isNotBlank();
    }

    /**
     * Two readings can share the newest timestamp — nothing in the schema stops
     * it — and the animal must still appear once, or the map would draw two
     * markers for one cow.
     */
    @Test
    void anAnimalWithTiedNewestReadingsAppearsOnce() {
        Farm farm = herdOf(1);
        Cow cow = cowRepository.findByFarmFarmId(farm.getFarmId()).get(0);

        LocalDateTime tie = LocalDateTime.now().plusMinutes(5);
        for (int i = 0; i < 2; i++) {
            LocationRecord duplicate = new LocationRecord();
            duplicate.setFarm(farm);
            duplicate.setCow(cow);
            duplicate.setLatitude(new BigDecimal("-23.9100"));
            duplicate.setLongitude(new BigDecimal("29.4700"));
            duplicate.setRecordedAt(tie);
            locationRepository.save(duplicate);
        }

        List<LiveCowResponse> live = farmContext.actingAsFarm(
                farm.getFarmId(), locationService::getLiveLocations);

        assertThat(live).hasSize(1);
    }
}
