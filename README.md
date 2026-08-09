<h1 align="center">Lostrego</h1>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green.svg" alt="License" /></a>
  <a href="#"><img src="https://img.shields.io/badge/java-25-ED8B00?logo=openjdk&logoColor=white" alt="Java 25" /></a>
  <a href="#"><img src="https://img.shields.io/badge/gradle-02303A?logo=gradle" alt="Gradle" /></a>
</p>

<p align="center">
  Cross-platform packet capture library for Java, using the FFM API to talk directly to native OS libraries. PDPK is a high-performance framework that bypasses the kernel and accesses the NIC directly via DMA for minimal latency.
</p>

---

## Purpose

This project is developed for **non-profit and educational purposes**, aiming to explore and implement the latest Java FFM API (Foreign Function & Memory) for native interoperability without JNI or JNA.

## Project Status

> **Phase 0** — Scaffolding and initial setup

- [ ] Core API: `PacketCapture`, `PacketListener`, `Packet` model
- [ ] Backend: libpcap (Linux/Mac)
- [ ] Backend: PDPK (Linux/Mac)
- [ ] Backend: npcap (Windows)

## Development Tools

| Tool | Description |
|---|---|
| [opencode](https://github.com/anomalyco/opencode) | AI coding assistant |
| [OpenSpec](https://github.com/Fission-AI/OpenSpec/) | Specification-driven development |

## Tech Stack

| Category | Technology | Version |
|---|---|---|
| **Language** | ![Java](https://img.shields.io/badge/-Java-ED8B00?logo=openjdk&logoColor=white) | 25 |
| **Build** | ![Gradle](https://img.shields.io/badge/-Gradle-02303A?logo=gradle) | — |
| **Native Interop** | FFM API (`java.lang.foreign`) | Standard in Java 25 |

All libraries and tools are **open source**, with no third-party license fees.

## License

Distributed under the **MIT License**.

You can use, modify, and distribute this software freely, including in proprietary projects. See [LICENSE](LICENSE) for details.
