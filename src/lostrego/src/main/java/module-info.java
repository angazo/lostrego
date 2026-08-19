module com.angazo.lostrego {
    exports com.angazo.lostrego.core;

    uses com.angazo.lostrego.core.spi.CaptureProvider;

    provides com.angazo.lostrego.core.spi.CaptureProvider
        with com.angazo.lostrego.backend.libpcap.LibpcapProvider;
}
