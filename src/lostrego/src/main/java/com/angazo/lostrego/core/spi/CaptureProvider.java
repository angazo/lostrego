package com.angazo.lostrego.core.spi;

import com.angazo.lostrego.core.CaptureConfig;
import com.angazo.lostrego.core.CaptureException;
import com.angazo.lostrego.core.PacketCapture;

/**
 * The service provider contract implemented by each capture backend (libpcap,
 * pdpk, npcap).
 *
 * <p>This package is an internal contract between {@code core} and the backends;
 * it is not exported and is not part of the public API.
 */
public interface CaptureProvider {

    /**
     * Returns the stable name of this backend, e.g. {@code "libpcap"}.
     *
     * @return the backend name
     */
    String name();

    /**
     * Returns whether this backend is usable on the current platform, i.e. its
     * native library is available.
     *
     * <p>An unavailable backend must return {@code false} rather than throwing,
     * so it never prevents other backends from working.
     *
     * @return {@code true} if this backend can be used here
     */
    boolean isSupported();

    /**
     * Opens a live capture session using this backend.
     *
     * @param config the capture configuration
     * @return a new capture session
     * @throws CaptureException if the session cannot be opened
     */
    PacketCapture openLive(CaptureConfig config) throws CaptureException;
}
