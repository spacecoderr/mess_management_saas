package com.thali.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import com.thali.TestProps;
import com.thali.common.exception.ApiException;
import com.thali.user.Role;
import com.thali.user.User;

class JwtServiceTest {

    private final JwtService jwt = new JwtService(TestProps.create());

    private User student() {
        User u = new User();
        u.setRole(Role.STUDENT);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, 42L);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return u;
    }

    @Test
    void accessTokenRoundTrips() {
        Jwt decoded = jwt.accessTokenDecoder().decode(jwt.issueAccessToken(student()));
        assertEquals("42", decoded.getSubject());
        assertEquals("STUDENT", decoded.getClaimAsString("role"));
    }

    @Test
    void registrationTokenCannotBeUsedAsAccessToken() {
        String reg = jwt.issueRegistrationToken("b423046@iiit-bh.ac.in");
        assertThrows(JwtException.class, () -> jwt.accessTokenDecoder().decode(reg));
        assertEquals("b423046@iiit-bh.ac.in", jwt.verifyRegistrationToken(reg));
    }

    @Test
    void accessTokenCannotBeUsedAsRegistrationToken() {
        String access = jwt.issueAccessToken(student());
        assertThrows(ApiException.class, () -> jwt.verifyRegistrationToken(access));
    }

    @Test
    void garbageTokenIsRejected() {
        assertThrows(ApiException.class, () -> jwt.verifyRegistrationToken("not.a.jwt"));
    }
}
