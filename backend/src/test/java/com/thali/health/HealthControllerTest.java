package com.thali.health;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {

    @Test
    void returnsUpWhenDatabaseReachable() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.isValid(2)).thenReturn(true);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HealthController(ds, "thali")).build();
        mvc.perform(get("/api/v1/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"))
           .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void returns503WhenDatabaseDown() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HealthController(ds, "thali")).build();
        mvc.perform(get("/api/v1/health"))
           .andExpect(status().isServiceUnavailable())
           .andExpect(jsonPath("$.database").value("DOWN"));
    }
}
