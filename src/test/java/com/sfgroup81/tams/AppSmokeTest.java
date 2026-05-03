package com.sfgroup81.tams;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AppSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    void bootstrapShouldNotThrow() {
        assertDoesNotThrow(() -> DataBootstrap.initialize(tempDir));
    }
}
