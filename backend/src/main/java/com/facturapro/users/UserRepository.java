package com.facturapro.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select case when count(u) > 0 then true else false end from User u")
    boolean existsAny();

    long countByActiveTrue();
}
