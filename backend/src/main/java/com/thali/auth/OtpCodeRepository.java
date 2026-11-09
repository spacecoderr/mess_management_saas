package com.thali.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    Optional<OtpCode> findFirstByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    long countByEmailAndCreatedAtAfter(String email, Instant since);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OtpCode o set o.consumed = true where o.email = :email and o.purpose = :purpose and o.consumed = false")
    int invalidateActive(@Param("email") String email, @Param("purpose") OtpPurpose purpose);
}
