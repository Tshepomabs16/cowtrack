package com.cowtrack.security;

import com.cowtrack.entity.Device;
import com.cowtrack.exception.UnauthorizedException;
import com.cowtrack.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Resolves a collar from the credentials it presents.
 *
 * <p>Devices cannot use the JWT flow: there is nobody to type a password, and a
 * token that expires in a day would strand a collar that spends a week out of
 * coverage. They present a long-lived key instead, stored hashed.
 *
 * <p>This is not a Spring Security filter. The ingestion paths are exempt from
 * the JWT chain and authenticate here instead, which keeps two unrelated
 * credential types from having to coexist in one filter chain — at the cost of
 * ingestion controllers having to call this explicitly rather than getting it
 * for free.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceAuthenticator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int KEY_BYTES = 32;

    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @return the device the credentials belong to
     * @throws UnauthorizedException if the serial is unknown, the key is wrong,
     *         or the device has been retired
     */
    @Transactional(readOnly = true)
    public Device authenticate(String serialNumber, String apiKey) {
        if (serialNumber == null || serialNumber.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("Device credentials are required");
        }

        Device device = deviceRepository.findBySerialNumber(serialNumber)
                .orElse(null);

        // An unknown serial and a wrong key give the same answer, so the response
        // cannot be used to enumerate which collars exist.
        if (device == null || !passwordEncoder.matches(apiKey, device.getApiKeyHash())) {
            log.warn("Rejected device credentials for serial {}", serialNumber);
            throw new UnauthorizedException("Invalid device credentials");
        }

        if (device.getStatus() != Device.Status.ACTIVE) {
            throw new UnauthorizedException("This device has been retired");
        }

        return device;
    }

    /**
     * Generates a key for a newly provisioned or re-keyed device.
     *
     * <p>Returned in the clear exactly once, at provisioning time, and only the
     * hash is kept — the same bargain as a password. A farmer who loses the key
     * rotates it rather than recovering it.
     */
    public String generateKey() {
        byte[] bytes = new byte[KEY_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String apiKey) {
        return passwordEncoder.encode(apiKey);
    }
}
