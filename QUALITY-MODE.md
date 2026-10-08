# Offline quality mode (build 101)

The setup button downloads HY-MT1.5-1.8B Q4_K_M (1,133,080,512 bytes) from Tencent, verifies SHA-256, and resumes interrupted downloads. Translation runs locally through a pinned llama.cpp CPU runtime. No account or API key is required for this model.

Quality mode waits for the specialist translation before journaling, showing and speaking the final result. Previous dialogue is supplied using the publisher's contextual prompt. User-corrected Memory AI entries remain authoritative. Native generation aborts after 18 seconds and rejects truncated output; the worker waits at most 20 seconds. A failed or rejected specialist result is explicitly labeled as a fallback. Old video segments are still dropped rather than queued without limit.

The first launch of this build disables the speed-first option and experimental voice cloning. These can be changed by the user. HY-MT accepts text only in this implementation; visual scene labels are not sent to it. Existing LiteRT imports remain optional.

Validation includes JVM prompt tests, native Android compilation, and real model inference on the CI host CPU. Host timings are not Redmi 13C timings and a few example sentences are not a translation-quality benchmark. Quality on real video and 6 GB memory behavior still require phone testing.
