package com.sfgroup81.tams;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AppSmokeTest {
    @Test
    void bootstrapShouldNotThrow() {
        assertDoesNotThrow(DataBootstrap::initialize);
    }
}
