package com.thali.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.thali.TestProps;

class EmailPolicyTest {

    private final EmailPolicy policy = new EmailPolicy(TestProps.create());

    @ParameterizedTest
    @ValueSource(strings = { "b423046@iiit-bh.ac.in", "B423046@IIIT-BH.AC.IN", "  b423046@iiit-bh.ac.in " })
    void acceptsCollegeEmails(String email) {
        assertTrue(policy.isCollegeStudentEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = { "b423046@gmail.com", "b42304@iiit-bh.ac.in", "423046@iiit-bh.ac.in",
            "b423046@mail.iiit-bh.ac.in", "b423046@iiit-bh.ac.in.evil.com", "x@y@iiit-bh.ac.in", "@iiit-bh.ac.in" })
    void rejectsEverythingElse(String email) {
        assertFalse(policy.isCollegeStudentEmail(email));
    }

    @Test
    void derivesRollNoAndBatchYear() {
        assertEquals("B423046", policy.rollNo("b423046@iiit-bh.ac.in"));
        assertEquals((short) 2023, policy.batchYear("B423046"));
    }
}
