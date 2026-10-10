package com.cowtrack.config;

import com.cowtrack.entity.*;
import com.cowtrack.repository.*;
import com.cowtrack.util.FenceShapes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cowtrack.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final String DEMO_EMAIL = "demo@cowtrack.dev";
    private static final String DEMO_PASSWORD = "demo1234";

    private static final double FARM_LAT = -23.9045;
    private static final double FARM_LNG = 29.4689;

    private static final int PRODUCTION_DAYS = 45;
    private static final int VITALS_DAYS = 14;
    private static final int FINANCIAL_MONTHS = 8;

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final CowRepository cowRepository;
    private final GeofenceRepository geofenceRepository;
    private final LocationRecordRepository locationRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthMetricRepository healthMetricRepository;
    private final VaccinationRepository vaccinationRepository;
    private final ProductionRecordRepository productionRepository;
    private final FinancialRecordRepository financialRepository;
    private final ReminderRepository reminderRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(20260826L);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Seed skipped: database already contains {} user(s)", userRepository.count());
            return;
        }

        log.info("Seeding demo data...");

        Farm farm = new Farm();
        farm.setFarmName("Green Pastures Farm");
        farm.setLocation("Polokwane, Limpopo");
        farm = farmRepository.save(farm);

        User farmer = createUser("Tshepo Maabane", DEMO_EMAIL, User.Role.FARMER,
                "082 123 4567", "Green Pastures Farm", farm);
        User caretaker = createUser("Naledi Mokoena", "naledi@cowtrack.dev", User.Role.CARETAKER,
                "083 555 0198", "Green Pastures Farm", farm);

        List<Cow> herd = createHerd(caretaker, farm);
        createGeofences(herd, farm);
        createLocationHistory(herd, farm);
        createVitals(herd, farm);
        createProduction(herd, farm);
        createHealthRecords(herd, caretaker, farm);
        createVaccinations(herd, farm);
        createReminders(herd, farm);
        createFinancials(farmer, farm);
        createAlerts(herd, farm);

        log.info("Seed complete: {} users, {} cows, {} location records, {} vitals, "
                        + "{} production records, {} financial records, {} alerts",
                userRepository.count(), cowRepository.count(), locationRepository.count(),
                healthMetricRepository.count(), productionRepository.count(),
                financialRepository.count(), alertRepository.count());
        log.info("Sign in with {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private User createUser(String name, String email, User.Role role, String phone, String farmName, Farm farm) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setPhone(phone);
        user.setFarmName(farmName);
        user.setLocation("Polokwane, Limpopo");
        user.setTimezone("Africa/Johannesburg");
        user.setLanguage("en");
        user.setCreatedAt(LocalDateTime.now().minusMonths(10));
        user.setFarms(new HashSet<>());
        User savedUser = userRepository.save(user);
        savedUser.getFarms().add(farm);
        return userRepository.save(savedUser);
    }

    private List<Cow> createHerd(User caretaker, Farm farm) {
        String[][] spec = {
                {"CT-001", "Bessie",    "Holstein", "2020-03-14"},
                {"CT-002", "Daisy",     "Jersey",   "2019-07-02"},
                {"CT-003", "Moo",       "Angus",    "2021-01-25"},
                {"CT-004", "Buttercup", "Holstein", "2020-11-08"},
                {"CT-005", "Clover",    "Hereford", "2018-05-19"},
                {"CT-006", "Nandi",     "Nguni",    "2021-09-30"},
                {"CT-007", "Thandi",    "Nguni",    "2022-02-11"},
                {"CT-008", "Rosie",     "Jersey",   "2019-12-05"},
                {"CT-009", "Mabel",     "Holstein", "2022-06-21"},
                {"CT-010", "Zola",      "Angus",    "2021-04-17"},
                {"CT-011", "Precious",  "Hereford", "2020-08-29"},
                {"CT-012", "Lerato",    "Nguni",    "2023-01-09"},
        };

        List<Cow> herd = new ArrayList<>();
        for (String[] row : spec) {
            Cow cow = new Cow();
            cow.setTagId(row[0]);
            cow.setName(row[1]);
            cow.setBreed(row[2]);
            cow.setDateOfBirth(LocalDate.parse(row[3]));
            cow.setCaretaker(caretaker);
            cow.setFarm(farm);
            cow.setCreatedAt(LocalDateTime.now().minusMonths(6));
            herd.add(cowRepository.save(cow));
        }

        link(herd, 8, 0);
        link(herd, 11, 5);
        return herd;
    }

    private void link(List<Cow> herd, int calfIndex, int motherIndex) {
        Cow calf = herd.get(calfIndex);
        calf.setMother(herd.get(motherIndex));
        cowRepository.save(calf);
    }

    /**
     * One camp the whole herd grazes in, a dam they must keep out of, and a
     * second camp resting in the rotation, so the map shows each kind.
     */
    private void createGeofences(List<Cow> herd, Farm farm) {
        LocalDateTime drawn = LocalDateTime.now().minusMonths(6);

        Geofence home = new Geofence();
        home.setFarm(farm);
        home.setName("Home camp");
        home.setFenceType(Geofence.FenceType.KEEP_IN);
        home.setShape(Geofence.Shape.CIRCLE);
        home.setCenterLatitude(BigDecimal.valueOf(FARM_LAT));
        home.setCenterLongitude(BigDecimal.valueOf(FARM_LNG));
        home.setRadiusMeters(800);
        home.setCreatedAt(drawn);
        home = geofenceRepository.save(home);

        for (Cow cow : herd) {
            cow.setCamp(home);
            cowRepository.save(cow);
        }

        Geofence dam = new Geofence();
        dam.setFarm(farm);
        dam.setName("Dam");
        dam.setDescription("Steep banks; animals have got stuck in the mud");
        dam.setFenceType(Geofence.FenceType.KEEP_OUT);
        dam.setShape(Geofence.Shape.CIRCLE);
        dam.setCenterLatitude(BigDecimal.valueOf(FARM_LAT + 0.0045));
        dam.setCenterLongitude(BigDecimal.valueOf(FARM_LNG - 0.0040));
        dam.setRadiusMeters(60);
        dam.setCreatedAt(drawn);
        geofenceRepository.save(dam);

        Geofence north = new Geofence();
        north.setFarm(farm);
        north.setName("North camp");
        north.setDescription("Resting until the rains");
        north.setFenceType(Geofence.FenceType.KEEP_IN);
        north.setShape(Geofence.Shape.POLYGON);
        north.setVerticesJson(FenceShapes.toJson(List.of(
                corner(FARM_LAT + 0.0090, FARM_LNG - 0.0060),
                corner(FARM_LAT + 0.0090, FARM_LNG + 0.0060),
                corner(FARM_LAT + 0.0170, FARM_LNG + 0.0050),
                corner(FARM_LAT + 0.0160, FARM_LNG - 0.0070))));
        north.setIsActive(false);
        north.setCreatedAt(drawn);
        geofenceRepository.save(north);
    }

    private static List<BigDecimal> corner(double lat, double lng) {
        return List.of(BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
    }

    private void createLocationHistory(List<Cow> herd, Farm farm) {
        for (int i = 0; i < herd.size(); i++) {
            Cow cow = herd.get(i);
            boolean staleCollar = (i == 6 || i == 10);

            for (int step = 11; step >= 0; step--) {
                LocationRecord record = new LocationRecord();
                record.setCow(cow);
                record.setLatitude(jitter(FARM_LAT, 0.004));
                record.setLongitude(jitter(FARM_LNG, 0.004));
                record.setAccuracy(BigDecimal.valueOf(5 + random.nextInt(12)));
                record.setRecordedAt(staleCollar
                        ? LocalDateTime.now().minusDays(3).minusHours(step * 2L)
                        : LocalDateTime.now().minusHours(step * 2L));
                record.setFarm(farm);
                locationRepository.save(record);
            }
        }
    }

    private void createVitals(List<Cow> herd, Farm farm) {
        for (int i = 0; i < herd.size(); i++) {
            Cow cow = herd.get(i);
            boolean unwell = (i == 2 || i == 7);

            for (int day = VITALS_DAYS; day >= 0; day--) {
                HealthMetric metric = new HealthMetric();
                metric.setCow(cow);
                metric.setTemperature(unwell && day < 3
                        ? round(38.9 + random.nextDouble() * 0.5, 1)
                        : round(38.0 + random.nextDouble() * 0.45, 1));
                metric.setHeartRate(unwell && day < 3
                        ? round(72 + random.nextDouble() * 8, 1)
                        : round(58 + random.nextDouble() * 11, 1));
                metric.setActivityLevel(unwell && day < 3
                        ? round(30 + random.nextDouble() * 15, 1)
                        : round(62 + random.nextDouble() * 33, 1));
                metric.setRecordedAt(LocalDateTime.now().minusDays(day).withHour(6).withMinute(30));
                metric.setFarm(farm);
                healthMetricRepository.save(metric);
            }
        }
    }

    private void createProduction(List<Cow> herd, Farm farm) {
        for (Cow cow : herd) {
            double base = switch (cow.getBreed()) {
                case "Holstein" -> 30;
                case "Jersey" -> 22;
                case "Nguni" -> 12;
                default -> 0;
            };
            if (base == 0) {
                continue;
            }

            double weight = 400 + random.nextInt(90);
            for (int day = PRODUCTION_DAYS; day >= 0; day--) {
                weight += random.nextDouble() * 0.6 - 0.15;

                ProductionRecord record = new ProductionRecord();
                record.setCow(cow);
                record.setRecordDate(LocalDate.now().minusDays(day));
                record.setMilkLitres(round(base + random.nextDouble() * 8 - 4, 1));
                record.setWeightKg(round(weight, 1));
                record.setCreatedAt(LocalDateTime.now().minusDays(day));
                record.setFarm(farm);
                productionRepository.save(record);
            }
        }
    }

    private void createHealthRecords(List<Cow> herd, User vet, Farm farm) {
        String[][] entries = {
                {"Routine checkup", "No issues found; condition good"},
                {"Mild lameness", "Hoof trimmed and dressed, rested for five days"},
                {"Mastitis, left quarter", "Intramammary antibiotic course, milk withheld"},
                {"Low body condition", "Supplementary feed and mineral lick introduced"},
        };

        for (int i = 0; i < herd.size(); i++) {
            String[] entry = entries[i % entries.length];
            HealthRecord record = new HealthRecord();
            record.setCow(herd.get(i));
            record.setDiagnosis(entry[0]);
            record.setTreatment(entry[1]);
            record.setVetName("Dr. Sipho Ndlovu");
            record.setRecordDate(LocalDate.now().minusDays(9L + i * 5L));
            record.setCreatedAt(LocalDateTime.now().minusDays(9L + i * 5L));
            record.setFarm(farm);
            healthRecordRepository.save(record);
        }
    }

    private void createVaccinations(List<Cow> herd, Farm farm) {
        String[] vaccines = {"Brucellosis", "Lumpy skin disease", "Anthrax", "Botulism"};

        for (int i = 0; i < herd.size(); i++) {
            Cow cow = herd.get(i);

            Vaccination done = new Vaccination();
            done.setCow(cow);
            done.setVaccineName(vaccines[i % vaccines.length]);
            done.setAdministeredDate(LocalDate.now().minusMonths(5).plusDays(i));
            done.setNextDueDate(LocalDate.now().plusMonths(7).plusDays(i));
            done.setVetName("Dr. Sipho Ndlovu");
            done.setNotes("Annual programme");
            done.setFarm(farm);
            vaccinationRepository.save(done);

            if (i % 3 == 0) {
                Vaccination upcoming = new Vaccination();
                upcoming.setCow(cow);
                upcoming.setVaccineName(vaccines[(i + 1) % vaccines.length]);
                upcoming.setNextDueDate(i < 4
                        ? LocalDate.now().minusDays(6L + i)
                        : LocalDate.now().plusDays(12L + i));
                upcoming.setVetName("Dr. Sipho Ndlovu");
                upcoming.setFarm(farm);
                vaccinationRepository.save(upcoming);
            }
        }
    }

    private void createReminders(List<Cow> herd, Farm farm) {
        String[][] spec = {
                {"Vaccination",   "MONTHLY", "-2", "Lumpy skin booster due"},
                {"Hoof trimming", "MONTHLY", "0",  "Check front hooves"},
                {"Weigh-in",      "WEEKLY",  "3",  "Monthly weight tracking"},
                {"Deworming",     "MONTHLY", "9",  "Rotate the active ingredient"},
                {"Pregnancy scan","MONTHLY", "16", "Confirm service from last cycle"},
        };

        for (int i = 0; i < spec.length; i++) {
            Reminder reminder = new Reminder();
            reminder.setCow(herd.get(i));
            reminder.setReminderType(spec[i][0]);
            reminder.setFrequency(Reminder.Frequency.valueOf(spec[i][1]));
            reminder.setStartDate(LocalDate.now().plusDays(Long.parseLong(spec[i][2])));
            reminder.setCreatedAt(LocalDateTime.now().minusDays(20));
            reminder.setFarm(farm);
            reminderRepository.save(reminder);
        }
    }

    private void createFinancials(User owner, Farm farm) {
        String[][] costs = {
                {"Feed", "11800"}, {"Veterinary", "2600"}, {"Labour", "7400"},
                {"Maintenance", "1900"}, {"Utilities", "1450"},
        };

        for (int month = FINANCIAL_MONTHS; month >= 0; month--) {
            LocalDate when = LocalDate.now().minusMonths(month).withDayOfMonth(15);

            save(owner, FinancialRecord.EntryType.REVENUE,
                    round(52000 + random.nextDouble() * 18000, 2), "Milk sales",
                    "Bulk collection", when, farm);

            if (month % 3 == 0) {
                save(owner, FinancialRecord.EntryType.REVENUE,
                        round(9000 + random.nextDouble() * 6000, 2), "Livestock sales",
                        "Weaner sale", when.plusDays(4), farm);
            }

            for (String[] cost : costs) {
                double amount = Double.parseDouble(cost[1]) * (0.85 + random.nextDouble() * 0.3);
                save(owner, FinancialRecord.EntryType.COST, round(amount, 2), cost[0],
                        cost[0] + " for " + when.getMonth(), when, farm);
            }
        }
    }

    private void save(User owner, FinancialRecord.EntryType type, BigDecimal amount,
                      String category, String description, LocalDate when, Farm farm) {
        FinancialRecord record = new FinancialRecord();
        record.setUser(owner);
        record.setEntryType(type);
        record.setAmount(amount);
        record.setCategory(category);
        record.setDescription(description);
        record.setRecordDate(when);
        record.setCreatedAt(when.atStartOfDay());
        record.setFarm(farm);
        financialRepository.save(record);
    }

    private void createAlerts(List<Cow> herd, Farm farm) {
        record Spec(int cowIndex, Alert.AlertType type, String message, int hoursAgo, boolean resolved) {}

        List<Spec> specs = List.of(
                new Spec(2, Alert.AlertType.GEOFENCE_BREACH,
                        "Cow 'Moo' (CT-003) left the grazing boundary", 2, false),
                new Spec(6, Alert.AlertType.NO_SIGNAL,
                        "No GPS fix from 'Thandi' (CT-007) for 72 hours", 5, false),
                new Spec(10, Alert.AlertType.NO_SIGNAL,
                        "No GPS fix from 'Precious' (CT-011) for 72 hours", 8, false),
                new Spec(7, Alert.AlertType.NIGHT_MOVEMENT,
                        "Unusual night movement from 'Rosie' (CT-008)", 20, false),
                new Spec(4, Alert.AlertType.DEVICE_REMOVED,
                        "Collar removed from 'Clover' (CT-005)", 30, true),
                new Spec(0, Alert.AlertType.GEOFENCE_BREACH,
                        "Cow 'Bessie' (CT-001) left the grazing boundary", 52, true)
        );

        for (Spec spec : specs) {
            Alert alert = new Alert();
            alert.setCow(herd.get(spec.cowIndex()));
            alert.setAlertType(spec.type());
            alert.setMessage(spec.message());
            alert.setIsResolved(spec.resolved());
            alert.setCreatedAt(LocalDateTime.now().minusHours(spec.hoursAgo()));
            alert.setFarm(farm);
            alertRepository.save(alert);
        }
    }

    private BigDecimal jitter(double centre, double spread) {
        return round(centre + (random.nextDouble() - 0.5) * 2 * spread, 6);
    }

    private BigDecimal round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP);
    }
}
