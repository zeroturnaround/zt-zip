# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/zeroturnaround/zt-zip/compare/zt-zip-1.17...HEAD
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
