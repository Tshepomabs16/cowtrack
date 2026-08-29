package com.cowtrack.repository;

import com.cowtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u JOIN u.farms f WHERE f.farmId = :farmId AND u.userId = :userId")
    Optional<User> findByFarmFarmIdAndUserId(@Param("farmId") Long farmId, @Param("userId") Long userId);
}