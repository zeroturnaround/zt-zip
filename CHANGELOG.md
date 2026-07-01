# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.18.1] - 2026-07-01

### Fixed

- `ZipUtil.unpack` no longer applies a ZIP entry's stored file permissions to the output directory itself when an entry's name resolves to it (for example an entry named `/`), which could change the output directory's permissions ([GHSA-v2g6-7r9j-v6px](https://github.com/zeroturnaround/zt-zip/security/advisories/GHSA-v2g6-7r9j-v6px)).

## [1.18.0] - 2026-07-01

### Added

- `pack` overloads that write to an existing `java.util.zip.ZipOutputStream`.

### Changed

- Raised the minimum runtime to Java 8 (bytecode target moved from 1.6 to 1.8).
- Upgraded the `slf4j-api` dependency from 1.6.6 to 2.0.18. zt-zip uses only the SLF4J API; applications that pick this newer API up transitively and still use an SLF4J 1.x binding must move to an SLF4J 2.x-compatible binding.
- `slf4j-api` is now a runtime-scoped dependency (previously `compile` scope), so it is no longer on the consumer compile classpath. Declare a direct `slf4j-api` dependency if your own code references SLF4J.

### Fixed

- `Zips` unpack with a transformer no longer hangs when the transformer produces no entry, and a transformer that throws now surfaces its real exception to the caller instead of a misleading "Write end dead" pipe error.
- `Zips.addEntry`/`addEntries` no longer fail with "Stream closed" when adding a directory `FileSource`; a directory is now stored as a proper directory entry ([#138](https://github.com/zeroturnaround/zt-zip/issues/138)).
- `ZipUtil.pack` no longer fails with `FileNotFoundException` when a directory contains a broken (dangling) symbolic link; such entries are skipped ([#122](https://github.com/zeroturnaround/zt-zip/issues/122)).
- `ZipUtil.packEntries`/`packEntry(File, File, NameMapper)` now skip an entry whose `NameMapper` returns `null` (the same convention as the directory `pack`) instead of throwing `NullPointerException` and leaving a partial zip.
- The `ZipUtil` methods that take a separate destination — `addEntry`/`addEntries`, `removeEntry`/`removeEntries`, `replaceEntry`/`replaceEntries`, `addOrReplaceEntries`, `transformEntry`/`transformEntries`, `repack` — now reject a destination equal to the source with an `IllegalArgumentException` instead of truncating and destroying the source before reading it; use the in-place variant (without a destination) instead.
- `ByteSource` (and the `byte[]` `ZipUtil.addEntry`/`replaceEntry` overloads) now accept `null` bytes as the documented directory entry instead of throwing `NullPointerException`.
- `ByteSource` with `null` bytes and the `STORED` method now produces a valid empty entry (size 0, CRC 0) instead of failing with "STORED entry missing size, compressed size, or crc-32".

### Security

- Hardened the relative path-traversal checks when unpacking, using `java.nio.file.Path` for consistent sub-directory containment checks.
- The `Zips` fluent API unpack path (`Zips.get(...).unpack().destination(...).process()`) now applies the same path-traversal guard as `ZipUtil.unpack`, rejecting entries that resolve outside the destination directory; this covers both the plain and transformer branches ([#180](https://github.com/zeroturnaround/zt-zip/pull/180)).
- In-place unpack now creates its temporary directory securely with `Files.createTempDirectory` (atomic, owner-only permissions) instead of a predictable, world-readable directory.
- `AsiExtraField` now validates the declared symbolic-link length against the bytes actually present before allocating, so a forged length in a crafted archive can no longer trigger a large (up to ~2 GB) memory allocation per entry while unpacking ([#181](https://github.com/zeroturnaround/zt-zip/pull/181)).
- `AsiExtraField` now rejects a truncated ASI extra field with a `ZipException` instead of letting an `ArrayIndexOutOfBoundsException`/`NegativeArraySizeException` abort unpacking, completing the bounds check added in [#181](https://github.com/zeroturnaround/zt-zip/pull/181).
- `BackslashUnpacker` now validates the resolved path before creating any directories, so a backslash-separated `..\` entry can no longer create directories outside the output directory (the file write itself was already blocked).
- `ZipUtil.explode`, `repack` and `unexplode` now create their working file or directory atomically (`File.createTempFile` / `Files.createTempDirectory`) instead of a predictable name next to the target, closing a symlink/TOCTOU race when the target sits in a shared directory; the predictable `FileUtils.getTempFileFor` helper is deprecated.
- `ZipUtil.unwrap` now throws a `ZipException` for an entry name whose path prefix resolves outside the name (such as a `~`- or `:`-prefixed name) instead of letting an unchecked `StringIndexOutOfBoundsException` abort the operation.

## [1.17] - 2024-01-28

### Added

- ProGuard rule to preserve `org.zeroturnaround.zip.extra.ZipExtraField.<init>`.

### Changed

- Exclude `rebel.xml` file from the release artefact.

## [1.15] - 2022-03-04

### Changed

- Ignore bad extra field entry error for permission extraction.

## [1.14] - 2020-02-10

### Added

- `removeEntries` that copies to an `OutputStream`.
- `createEmpty`.

## [1.13] - 2018-05-02

### Fixed

- Same-zip bug for the `transformEntry` method.

### Security

- Fixed a possible security vulnerability reported by the Snyk Security Research Team.

## [1.12] - 2017-08-01

### Fixed

- Resource leakage with `ZipInputStream`.
- `NoSuchMethodError` on Android platforms.

## [1.11] - 2017-01-31

### Added

- `iterate` and `unpack` methods that accept a `Charset`.

## [1.10] - 2016-10-28

### Added

- User-configurable compression level for `packEntries()`.
- More overloaded `pack()` methods for convenience.

### Changed

- Bumped embedded Apache Commons from 1.4 to 2.2.

## [1.9] - 2015-11-20

### Added

- Support for Java 7 POSIX file permissions.
- Ability to create and update byte-array backed ZIP streams.
- Ability to specify/replace the compression level of a `ZipEntry`.
- `BackslashUnpacker` for broken (Windows) ZIP archives.

### Changed

- Bumped minimal supported Java version to Java 5.

### Fixed

- Not closing the `InputStream` after processing each `ZipEntrySource`.
- Buffering when creating and updating ZIP streams.

## [1.8] - 2014-07-07

### Changed

- `ZipUtil.pack` is more memory efficient for large directories.
- Improved `Charset` support.

### Removed

- Dependency on commons-io.

### Fixed

- Preserving the compressed state of copied entries.
- Packing files from a directory based on an accept filter.

## [1.6-SNAPSHOT] - 2012-09-17

### Added

- Started to write a changelog.
- Public CI, https://travis-ci.org/.

[Unreleased]: https://github.com/zeroturnaround/zt-zip/compare/v1.18.1...HEAD
[1.18.1]: https://github.com/zeroturnaround/zt-zip/compare/v1.18.0...v1.18.1
[1.18.0]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.17...v1.18.0
[1.17]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.15...zt-zip-1.17
[1.15]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.14...zt-zip-1.15
[1.14]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.13...zt-zip-1.14
[1.13]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.12...zt-zip-1.13
[1.12]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.11...zt-zip-1.12
[1.11]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.10...zt-zip-1.11
[1.10]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.9...zt-zip-1.10
[1.9]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.8...zt-zip-1.9
[1.8]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.6...zt-zip-1.8
[1.6-SNAPSHOT]: https://github.com/zeroturnaround/zt-zip/releases/tag/zt-zip-1.6
