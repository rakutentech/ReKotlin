# ReKotlin

![ReKotlin Logo](https://rakutentech.github.io/ReKotlin/logo.svg)

[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://github.com/rakutentech/ReKotlin/blob/master/LICENSE)
[![CircleCI](https://circleci.com/gh/rakutentech/ReKotlin.svg?style=svg)](https://circleci.com/gh/rakutentech/ReKotlin)
[![codecov](https://codecov.io/gh/rakutentech/ReKotlin/branch/master/graph/badge.svg)](https://codecov.io/gh/rakutentech/ReKotlin)

Unidirectional data flow for Kotlin applications.

## Documentation

See https://rakutentech.github.io/ReKotlin/

## ReKotlin libraries

This repository is a collection of libraries that implement and leverage the unidirectional-data-flow model, checkout the sub directories for more detail:

- [ReKotlin](rekotlin): Implementation of the core concepts.
- [ReKotlin-Router](rekotlin-router): A routing library on top of ReKotlin, for expressive URL-like routing
- [Android Sample App](sample-android): A sample app illustrating how you can use ReKotlin and ReKotlin-Router in an android application.

## Building

```shell
./gradlew clean build
```

## Publishing Binaries

```shell
./gradlew publish
```

## Publishing Documentation

```shell
./docs.sh
```
