package com.angazo.lostrego.backend.libpcap;

import com.angazo.lostrego.core.CaptureConfig;
import com.angazo.lostrego.core.PacketCapture;
import com.angazo.lostrego.core.spi.CaptureProvider;

/**
 * {@link CaptureProvider} backed by libpcap, available on Linux and macOS.
 */
public final class LibpcapProvider implements CaptureProvider {

    @Override
    public String name() {
        return "libpcap";
    }

    @Override
    public boolean isSupported() {
        return LibpcapNative.isAvailable();
    }

    @Override
    public PacketCapture openLive(CaptureConfig config) {
        return LibpcapCapture.open(config);
    }
}
