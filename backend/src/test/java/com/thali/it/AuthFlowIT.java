package com.thali.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import com.thali.auth.OtpPurpose;
import com.thali.user.ApprovalStatus;
import com.thali.user.Role;
import com.thali.user.User;

class AuthFlowIT extends AbstractIntegrationTest {

    static final String EMAIL = "b423046@iiit-bh.ac.in";

    private ResultActions requestOtp(String email, OtpPurpose purpose) throws Exception {
        return post("/api/v1/auth/otp/request", null, "{\"email\":\"%s\",\"purpose\":\"%s\"}".formatted(email, purpose));
    }

    private ResultActions verifyOtp(String email, OtpPurpose purpose, String code) throws Exception {
        return post("/api/v1/auth/otp/verify", null, "{\"email\":\"%s\",\"purpose\":\"%s\",\"code\":\"%s\"}".formatted(email, purpose, code));
    }

    private ResultActions login(String email, String password) throws Exception {
        return post("/api/v1/auth/login", null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private String registrationToken(String email) throws Exception {
        requestOtp(email, OtpPurpose.REGISTER).andExpect(status().isOk());
        return read(verifyOtp(email, OtpPurpose.REGISTER, otps.lastCode(email, OtpPurpose.REGISTER)).andExpect(status().isOk()), "$.registrationToken");
    }

    private ResultActions register(String token, String password) throws Exception {
        return post("/api/v1/auth/register", null, """
                {"registrationToken":"%s","name":"Asha Rao","phone":"9876543210","hostelBlock":"B1","roomNo":"204","dietPref":"VEG","password":"%s"}
                """.formatted(token, password));
    }

    @Test
    void fullJourney_otpRegisterApproveLoginRefreshLogout() throws Exception {
        // 1-2. verify the college email by OTP, then complete the profile
        String token = registrationToken(EMAIL);
        register(token, PASSWORD).andExpect(status().isCreated())
                .andExpect(jsonPath("$.rollNo").value("B423046"))
                .andExpect(jsonPath("$.batchYear").value(2023))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // 3. cannot log in until an admin approves
        login(EMAIL, PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_PENDING_APPROVAL"));

        // 4. admin sees them in the queue and approves
        User admin = admin();
        get("/api/v1/students/pending", admin).andExpect(status().isOk()).andExpect(jsonPath("$.content[*].rollNo", hasItem("B423046")));
        long studentId = users.findByEmailIgnoreCase(EMAIL).orElseThrow().getId();
        patch("/api/v1/students/" + studentId + "/approval", admin, "{\"status\":\"APPROVED\"}").andExpect(status().isOk());

        // 5. login works; access token opens /me; refresh tokens rotate and are single-use
        ResultActions ok = login(EMAIL, PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("STUDENT"));
        String access = read(ok, "$.accessToken"), refresh = read(ok, "$.refreshToken");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL));

        ResultActions refreshed = post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isOk());
        String refresh2 = read(refreshed, "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh);
        post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));

        post("/api/v1/auth/logout", null, "{\"refreshToken\":\"" + refresh2 + "\"}").andExpect(status().isOk());
        post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refresh2 + "\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void onlyCollegeEmailsCanRegisterOrUseOtpLogin() throws Exception {
        for (String bad : new String[] {"someone@gmail.com", "b42304@iiit-bh.ac.in", "b423046@iiit-bh.ac.in.evil.com"}) {
            requestOtp(bad, OtpPurpose.REGISTER).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_COLLEGE_EMAIL"));
            assertThat(otps.wasSent(bad, OtpPurpose.REGISTER)).isFalse();
        }
        requestOtp("someone@gmail.com", OtpPurpose.LOGIN).andExpect(status().isBadRequest());
    }

    @Test
    void alreadyRegisteredEmailIsRefused() throws Exception {
        student("B423046");
        requestOtp(EMAIL, OtpPurpose.REGISTER).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void wrongCodesAreCountedAndFiveFailuresLockTheCode() throws Exception {
        requestOtp(EMAIL, OtpPurpose.REGISTER).andExpect(status().isOk());
        String real = otps.lastCode(EMAIL, OtpPurpose.REGISTER);
        String wrong = real.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) verifyOtp(EMAIL, OtpPurpose.REGISTER, wrong).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OTP_INVALID"));
        verifyOtp(EMAIL, OtpPurpose.REGISTER, real).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("OTP_TOO_MANY_ATTEMPTS"));
    }

    @Test
    void aCodeWorksOnlyOnce() throws Exception {
        requestOtp(EMAIL, OtpPurpose.REGISTER).andExpect(status().isOk());
        String code = otps.lastCode(EMAIL, OtpPurpose.REGISTER);
        verifyOtp(EMAIL, OtpPurpose.REGISTER, code).andExpect(status().isOk());
        verifyOtp(EMAIL, OtpPurpose.REGISTER, code).andExpect(status().isBadRequest());
    }

    @Test
    void requestingAnotherCodeImmediatelyIsRateLimited() throws Exception {
        requestOtp(EMAIL, OtpPurpose.REGISTER).andExpect(status().isOk());
        requestOtp(EMAIL, OtpPurpose.REGISTER).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));
    }

    @Test
    void passwordRulesAreEnforcedOnRegistration() throws Exception {
        String token = registrationToken(EMAIL);
        register(token, "short1").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        register(token, "onlyletters").andExpect(status().isBadRequest());
        assertThat(users.existsByEmailIgnoreCase(EMAIL)).isFalse();
    }

    @Test
    void registrationTokenIsRejectedWhenTampered() throws Exception {
        register("not-a-token", PASSWORD).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REGISTRATION_TOKEN_INVALID"));
    }

    @Test
    void pendingRejectedAndDisabledAccountsCannotLogIn() throws Exception {
        student("B423001", ApprovalStatus.PENDING);
        student("B423002", ApprovalStatus.REJECTED);
        User disabled = student("B423003");
        disabled.setActive(false);
        users.save(disabled);

        login("b423001@iiit-bh.ac.in", PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_PENDING_APPROVAL"));
        login("b423002@iiit-bh.ac.in", PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_REJECTED"));
        login("b423003@iiit-bh.ac.in", PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        student("B423001");
        login("b423001@iiit-bh.ac.in", "WrongPass1").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login("nobody@iiit-bh.ac.in", PASSWORD).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void otpLoginWorksForApprovedStudentsAndStaysSilentForUnknownEmails() throws Exception {
        student("B423001");
        requestOtp("b423001@iiit-bh.ac.in", OtpPurpose.LOGIN).andExpect(status().isOk());
        verifyOtp("b423001@iiit-bh.ac.in", OtpPurpose.LOGIN, otps.lastCode("b423001@iiit-bh.ac.in", OtpPurpose.LOGIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.auth.accessToken").isNotEmpty()).andExpect(jsonPath("$.auth.user.rollNo").value("B423001"));

        String ghost = "z999999@iiit-bh.ac.in";
        requestOtp(ghost, OtpPurpose.LOGIN).andExpect(status().isOk());   // same response as a real account...
        assertThat(otps.wasSent(ghost, OtpPurpose.LOGIN)).isFalse();      // ...but nothing is sent
    }

    @Test
    void passwordResetChangesThePasswordAndKillsExistingSessions() throws Exception {
        student("B423001");
        String email = "b423001@iiit-bh.ac.in";
        String refresh = read(login(email, PASSWORD).andExpect(status().isOk()), "$.refreshToken");

        requestOtp(email, OtpPurpose.RESET_PASSWORD).andExpect(status().isOk());
        post("/api/v1/auth/password/reset", null, "{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"BrandNew123\"}"
                .formatted(email, otps.lastCode(email, OtpPurpose.RESET_PASSWORD))).andExpect(status().isOk());

        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, "BrandNew123").andExpect(status().isOk());
        post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsReturnAProblemJsonWhenTheTokenIsMissingOrBad() throws Exception {
        get("/api/v1/auth/me", null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED")).andExpect(jsonPath("$.timestamp").exists());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aRegistrationTokenCannotBeUsedAsAnAccessToken() throws Exception {
        String reg = registrationToken(EMAIL);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me").header("Authorization", "Bearer " + reg))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJsonAndMissingFieldsGetTheStandardErrorShape() throws Exception {
        call(HttpMethod.POST, "/api/v1/auth/login", null, "{not json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        post("/api/v1/auth/login", null, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void healthIsPublicAndReportsTheDatabase() throws Exception {
        get("/api/v1/health", null).andExpect(status().isOk()).andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void adminCreatedStaffCanLogInButStudentsCannotBeCreatedThisWay() throws Exception {
        User admin = admin();
        post("/api/v1/users", admin, """
                {"name":"Ramesh","email":"ramesh@thali.local","role":"STAFF","password":"Staff12345"}""").andExpect(status().isCreated());
        login("ramesh@thali.local", "Staff12345").andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("STAFF"));
        post("/api/v1/users", admin, """
                {"name":"X","email":"x@thali.local","role":"STUDENT","password":"Staff12345"}""").andExpect(status().isBadRequest());
        assertThat(users.findByEmailIgnoreCase("ramesh@thali.local").orElseThrow().getRole()).isEqualTo(Role.STAFF);
    }
}
