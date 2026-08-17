package com.angazo.lostrego.core.spi;

import com.angazo.lostrego.core.CaptureConfig;
import com.angazo.lostrego.core.CaptureException;
import com.angazo.lostrego.core.PacketCapture;
import com.angazo.lostrego.core.PacketCaptures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PacketCapturesTest {

    @Test
    void opensLiveWithFirstSupportedBackend() {
        FakeCaptureProvider.supported = true;
        var config = CaptureConfig.builder().device("eth0").build();

        try (PacketCapture capture = PacketCaptures.openLive(config)) {
            assertInstanceOf(FakeCapture.class, capture);
        }
    }

    @Test
    void opensLiveWithNamedBackend() {
        FakeCaptureProvider.supported = true;
        var config = CaptureConfig.builder().device("eth0").build();

        try (PacketCapture capture = PacketCaptures.openLive(config, "fake")) {
            assertInstanceOf(FakeCapture.class, capture);
        }
    }

    @Test
    void unknownBackendThrows() {
        FakeCaptureProvider.supported = true;
        var config = CaptureConfig.builder().device("eth0").build();

        assertThrows(CaptureException.class, () -> PacketCaptures.openLive(config, "nope"));
    }

    @Test
    void unavailableBackendThrows() {
        FakeCaptureProvider.supported = false;
        try {
            var config = CaptureConfig.builder().device("eth0").build();
            assertThrows(CaptureException.class, () -> PacketCaptures.openLive(config, "fake"));
        } finally {
            FakeCaptureProvider.supported = true;
        }
    }

    @Test
    void noSupportedBackendThrows() {
        FakeCaptureProvider.supported = false;
        try {
            var config = CaptureConfig.builder().device("eth0").build();
            assertThrows(CaptureException.class, () -> PacketCaptures.openLive(config));
        } finally {
            FakeCaptureProvider.supported = true;
        }
    }
}
