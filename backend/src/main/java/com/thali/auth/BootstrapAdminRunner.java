package com.thali.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.thali.config.AppProperties;
import com.thali.user.Role;
import com.thali.user.User;
import com.thali.user.UserRepository;

/** Creates the first admin from configuration when the database has none, so someone can approve students. */
@Order(1)
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties.Bootstrap cfg;

    public BootstrapAdminRunner(UserRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.cfg = props.bootstrap();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cfg.adminEmail() == null || cfg.adminEmail().isBlank() || cfg.adminPassword() == null || cfg.adminPassword().isBlank()) {
            return;
        }
        if (users.existsByRole(Role.ADMIN)) return;
        User admin = new User();
        admin.setName("Mess Admin");
        admin.setEmail(cfg.adminEmail().trim().toLowerCase());
        admin.setPasswordHash(encoder.encode(cfg.adminPassword()));
        admin.setRole(Role.ADMIN);
        admin.setEmailVerified(true);
        users.save(admin);
        log.info("Bootstrapped admin account {}", admin.getEmail());
    }
}
