package com.thali.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.thali.TestProps;
import com.thali.auth.otp.OtpSender;
import com.thali.auth.otp.OtpService;
import com.thali.common.exception.ApiException;
import com.thali.support.Fixtures;

/** Limits under test: 5 min lifetime, 5 attempts, 60 s resend cooldown, 5 codes per hour. */
class OtpServiceTest {

    static final String EMAIL = "b423046@iiit-bh.ac.in";

    OtpCodeRepository repo = mock(OtpCodeRepository.class);
    OtpSender sender = mock(OtpSender.class);
    PasswordEncoder encoder = new BCryptPasswordEncoder(4);   // cheap rounds keep the test fast
    OtpService service;

    @BeforeEach
    void setUp() {
        service = new OtpService(repo, sender, encoder, TestProps.create());
        when(repo.save(any(OtpCode.class))).thenAnswer(i -> i.getArgument(0));
        when(repo.findFirstByEmailAndPurposeOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.empty());
    }

    private static void assertCode(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", code);
    }

    /** Issues a code and returns {plainCode, storedEntity}. */
    private Object[] issue() {
        service.issue(EMAIL, OtpPurpose.LOGIN);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OtpCode> saved = ArgumentCaptor.forClass(OtpCode.class);
        verify(sender).send(eq(EMAIL), code.capture(), eq(OtpPurpose.LOGIN), anyInt());
        verify(repo).save(saved.capture());
        return new Object[] {code.getValue(), saved.getValue()};
    }

    private void active(OtpCode otp) {
        when(repo.findFirstByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(EMAIL, OtpPurpose.LOGIN)).thenReturn(Optional.of(otp));
    }

    @Test
    void issuesASixDigitCodeAndStoresOnlyItsHash() {
        Object[] r = issue();
        String code = (String) r[0];
        OtpCode stored = (OtpCode) r[1];
        assertThat(code).matches("\\d{6}");
        assertThat(stored.getCodeHash()).isNotEqualTo(code).startsWith("$2");
        assertThat(encoder.matches(code, stored.getCodeHash())).isTrue();
        assertThat(stored.getExpiresAt()).isBetween(Instant.now().plus(4, ChronoUnit.MINUTES), Instant.now().plus(6, ChronoUnit.MINUTES));
        verify(repo).invalidateActive(EMAIL, OtpPurpose.LOGIN);   // older codes stop working
    }

    @Test
    void resendWithinCooldownIsRejectedAndNothingIsSent() {
        OtpCode last = new OtpCode();
        Fixtures.createdAt(last, Instant.now().minusSeconds(10));
        when(repo.findFirstByEmailAndPurposeOrderByCreatedAtDesc(EMAIL, OtpPurpose.LOGIN)).thenReturn(Optional.of(last));
        assertCode(() -> service.issue(EMAIL, OtpPurpose.LOGIN), "OTP_COOLDOWN");
        verify(sender, never()).send(any(), any(), any(), anyInt());
    }

    @Test
    void resendAfterCooldownIsAllowed() {
        OtpCode last = new OtpCode();
        Fixtures.createdAt(last, Instant.now().minusSeconds(61));
        when(repo.findFirstByEmailAndPurposeOrderByCreatedAtDesc(EMAIL, OtpPurpose.LOGIN)).thenReturn(Optional.of(last));
        service.issue(EMAIL, OtpPurpose.LOGIN);
        verify(sender).send(eq(EMAIL), any(), eq(OtpPurpose.LOGIN), anyInt());
    }

    @Test
    void hourlyLimitStopsOtpFlooding() {
        when(repo.countByEmailAndCreatedAtAfter(eq(EMAIL), any())).thenReturn(5L);
        assertCode(() -> service.issue(EMAIL, OtpPurpose.LOGIN), "OTP_HOURLY_LIMIT");
        verify(sender, never()).send(any(), any(), any(), anyInt());
    }

    @Test
    void correctCodeIsAcceptedOnceAndThenConsumed() {
        Object[] r = issue();
        OtpCode otp = (OtpCode) r[1];
        active(otp);
        service.verifyAndConsume(EMAIL, OtpPurpose.LOGIN, (String) r[0]);
        assertThat(otp.isConsumed()).isTrue();
    }

    @Test
    void wrongCodeCountsAnAttemptAndStaysUsable() {
        Object[] r = issue();
        OtpCode otp = (OtpCode) r[1];
        active(otp);
        assertCode(() -> service.verifyAndConsume(EMAIL, OtpPurpose.LOGIN, "000000".equals(r[0]) ? "111111" : "000000"), "OTP_INVALID");
        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(otp.isConsumed()).isFalse();
    }

    @Test
    void afterFiveWrongAttemptsEvenTheRightCodeIsLocked() {
        Object[] r = issue();
        OtpCode otp = (OtpCode) r[1];
        otp.setAttempts(5);
        active(otp);
        assertCode(() -> service.verifyAndConsume(EMAIL, OtpPurpose.LOGIN, (String) r[0]), "OTP_TOO_MANY_ATTEMPTS");
        assertThat(otp.isConsumed()).isTrue();
    }

    @Test
    void expiredCodeIsRejectedEvenIfCorrect() {
        Object[] r = issue();
        OtpCode otp = (OtpCode) r[1];
        otp.setExpiresAt(Instant.now().minusSeconds(1));
        active(otp);
        assertCode(() -> service.verifyAndConsume(EMAIL, OtpPurpose.LOGIN, (String) r[0]), "OTP_INVALID");
        assertThat(otp.isConsumed()).isTrue();
    }

    @Test
    void verifyingWithoutAnyActiveCodeFails() {
        when(repo.findFirstByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.empty());
        assertCode(() -> service.verifyAndConsume(EMAIL, OtpPurpose.LOGIN, "123456"), "OTP_INVALID");
    }
}
