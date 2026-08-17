package com.angazo.lostrego.core;

import com.angazo.lostrego.core.spi.CaptureProvider;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Entry point of the library: resolves a capture backend in runtime and opens
 * capture sessions.
 *
 * <p>Backends are discovered with {@link ServiceLoader}; {@code core} has no
 * compile-time knowledge of the concrete backends. The returned
 * {@link PacketCapture} is fully backend-transparent: callers only ever deal
 * with the types in {@code com.angazo.lostrego.core}.
 */
public final class PacketCaptures {

    private static final List<CaptureProvider> PROVIDERS = loadProviders();

    private PacketCaptures() {
    }

    private static List<CaptureProvider> loadProviders() {
        return ServiceLoader.load(CaptureProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    /**
     * Opens a live capture session using the first available backend.
     *
     * @param config the capture configuration
     * @return a capture session backed by an available backend
     * @throws CaptureException     if no backend is supported on this platform
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public static PacketCapture openLive(CaptureConfig config) {
        Objects.requireNonNull(config, "config");
        return PROVIDERS.stream()
                .filter(CaptureProvider::isSupported)
                .findFirst()
                .orElseThrow(() -> new CaptureException(
                        "No capture backend is supported on this platform"))
                .openLive(config);
    }

    /**
     * Opens a live capture session using the named backend.
     *
     * @param config  the capture configuration
     * @param backend the backend name, e.g. {@code "libpcap"}
     * @return a capture session backed by the named backend
     * @throws CaptureException     if the backend is not registered or not supported here
     * @throws NullPointerException if {@code config} or {@code backend} is {@code null}
     */
    public static PacketCapture openLive(CaptureConfig config, String backend) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(backend, "backend");
        return PROVIDERS.stream()
                .filter(provider -> provider.name().equals(backend))
                .findFirst()
                .filter(CaptureProvider::isSupported)
                .orElseThrow(() -> new CaptureException(
                        "Capture backend '" + backend + "' is not available on this platform"))
                .openLive(config);
    }
}
