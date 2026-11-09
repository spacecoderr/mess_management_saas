package com.thali.user;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    /** `q` must already be lower-cased and wrapped in % (use "%" for no filter). */
    @Query("""
            select u from User u
            where u.role in :roles
              and (lower(u.name) like :q or lower(u.email) like :q)
            """)
    Page<User> search(@Param("roles") Collection<Role> roles, @Param("q") String q, Pageable pageable);
}
