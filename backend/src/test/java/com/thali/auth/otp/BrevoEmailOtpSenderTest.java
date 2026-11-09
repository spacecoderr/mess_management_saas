package com.thali.auth.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.thali.TestProps;
import com.thali.auth.OtpPurpose;
import com.thali.common.exception.ApiException;
import com.thali.config.AppProperties;

class BrevoEmailOtpSenderTest {

    private final AppProperties props = new AppProperties("Asia/Kolkata", TestProps.create().auth(), TestProps.create().jwt(),
            new AppProperties.Mail("test-key", "mess@example.com", "Thali Mess"), TestProps.create().bootstrap(),
            TestProps.create().attendance(), TestProps.create().billing(), TestProps.create().seed());

    @Test
    void sendsTheCodeThroughBrevoWithTheApiKeyAndSender() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.sender.email").value("mess@example.com"))
                .andExpect(jsonPath("$.to[0].email").value("b423046@iiit-bh.ac.in"))
                .andExpect(jsonPath("$.subject").value("Your Thali verification code: 123456"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        new BrevoEmailOtpSender(builder, props).send("b423046@iiit-bh.ac.in", "123456", OtpPurpose.LOGIN, 5);

        server.verify();
    }

    @Test
    void aBrevoFailureBecomesAnApiErrorSoTheOtpIsNotKept() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("https://api.brevo.com/v3/smtp/email")).andRespond(withServerError());

        assertThatThrownBy(() -> new BrevoEmailOtpSender(builder, props).send("a@b.c", "123456", OtpPurpose.REGISTER, 5))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "OTP_DELIVERY_FAILED");
    }

    @Test
    void theProductionConstructorWorksWithoutAnyInjectedBuilder() {
        assertThat(new BrevoEmailOtpSender(props)).isNotNull();   // the exact thing that crashed on Render
    }
}
