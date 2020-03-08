# ReKotlin

[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://github.com/rakutentech/ReKotlin/blob/master/LICENSE)
[![CircleCI](https://circleci.com/gh/rakutentech/ReKotlin.svg?style=svg)](https://circleci.com/gh/rakutentech/ReKotlin)
[![codecov](https://codecov.io/gh/rakutentech/ReKotlin/branch/master/graph/badge.svg)](https://codecov.io/gh/rakutentech/ReKotlin)

Unidirectional data flow for Kotlin applications.

## Introduction

ReKotlin an implementation of the unidirectional data flow architecture in Kotlin. It is heavily inspired by [Redux](https://github.com/reactjs/redux) but adds some tweaks to the formula.

ReKotlin helps you to separate three important concerns of your app's components:

- **State**: The entire application state is explicitly modelled data. This helps avoid complicated state management code, enables better debugging, prevents statefulness, and has many, many more benefits...
- **State Subscribers**: The application updates when your state changes - be that UI, processing state or networking. The UI layer becomes simple, deterministic rendering of the current application state.
- **State Changes**: The only way to state changes is through actions. Actions are small pieces of data that describe a state change. By drastically limiting the way state can be mutated, your app becomes easier to understand and it gets easier to work with many collaborators.

## Conceptual Model

ReKotlin relies on a few core concepts:

- **`Store`** maintains your entire app state in the form of a single data structure. This state can only be modified by dispatching Actions to the store. Whenever the state in the store changes, the store will notify all observers.
- **`Action`** are a declarative way of describing a state change. Actions don't contain any code, they are consumed by the store (or rather its reducers).
- **`Reducer`** are pure functions, implement the state transitinon on the current action and the current app state, creating the next app state

![](docs/img/reswift_concept.png)

## ReKotlin libraries

This repository is a collection of libraries that implement and leverage the unidirectional-data-flow model, checkout the sub directories for more detail:

- [ReKotlin](rekotlin): Implementation of the core concept
- [ReKotlin-Router](rekotlin-router): A routing library on top of ReKotlin, for expressive URL-like routing
- [Android Sample App](sample): A sample illustrating how you can use ReKotlin and ReKotlin-Router in an android application.
