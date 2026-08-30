package com.cowtrack.repository;

import com.cowtrack.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * Deliberately not farm-scoped: a device presents only its serial and key, so
     * the farm is the result of the lookup rather than an input to it. Every
     * other query here is scoped.
     */
    Optional<Device> findBySerialNumber(String serialNumber);

    List<Device> findByFarmFarmId(Long farmId);

    Optional<Device> findByFarmFarmIdAndDeviceId(Long farmId, Long deviceId);

    Optional<Device> findByFarmFarmIdAndCowCowId(Long farmId, Long cowId);

    boolean existsBySerialNumber(String serialNumber);
}
