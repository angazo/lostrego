<h1 align="center">Lostrego</h1>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green.svg" alt="License" /></a>
  <a href="#"><img src="https://img.shields.io/badge/java-25-ED8B00?logo=openjdk&logoColor=white" alt="Java 25" /></a>
  <a href="#"><img src="https://img.shields.io/badge/gradle-02303A?logo=gradle" alt="Gradle" /></a>
</p>

<p align="center">
  Cross-platform packet capture library for Java, using the FFM API to talk directly to native OS libraries such as <strong>libpcap</strong> on Linux/macOS, <strong>PDPK</strong> on Linux, and <strong>npcap</strong> on Windows.
</p>

---

## Backends

Lostrego talks to different native libraries depending on the platform:

| Backend | Platform | Description |
|---|---|---|
| [libpcap](https://www.tcpdump.org/) | Linux, macOS | Standard packet capture library on Unix. Provides an API to capture live packets, read/write `.pcap` files, and apply BPF filters. |
| [PDPK](https://www.dpdk.org/) | Linux | Data Plane Development Kit. High-performance framework that bypasses the kernel and accesses the NIC directly via DMA, achieving near-wire-speed capture with minimal latency. |
| [npcap](https://npcap.com/) | Windows | Packet capture library for Windows, compatible with libpcap APIs. Provides raw packet capture and injection on Windows networks. |

## Purpose

This project is developed for **non-profit and educational purposes**, aiming to explore and implement the latest Java FFM API (Foreign Function & Memory) for native interoperability without JNI or JNA.

## Project Status

> **Phase 0** — Scaffolding and initial setup

- [x] Gradle build skeleton and CI pipeline
- [ ] Backend: libpcap (Linux/Mac)
- [ ] Backend: PDPK (Linux)
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
