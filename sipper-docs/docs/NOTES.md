# Notes

Dated observations and corrections from building and testing this codebase.

---

## 2026-09-10

### gate 4 never covered :collect

ARCHITECTURE §3 asserted that the API-surface gate kept `:collect`'s return-type rule true, but §10
gate 4 only ever named the audit and usage `.api` files, and binary-compatibility-validator 0.16.3
registers no apiDump or apiCheck task for an Android library module, so no `.api` file for `:collect`
can exist. The claim was unsupported from the start. It is replaced by a source-level gate in
`collect/build.gradle.kts`, `checkCollectReturnsReading`, which was observed failing on a
deliberately planted `public fun temporaryBareDouble(): Double` before being trusted. Gate 4 remains
unchanged and continues to guard `:audit` and `:usage`. The source-level gate's stated limitation
is that it matches single-line signatures, which every function in `:collect` currently is.
