---
title: 'ReKotlin Overview'
date: 2018-11-28T15:14:39+10:00
weight: 1
---

ReKotlin is a library that enables you to implement unidirectional data flow for Kotlin applications. Originally it was a port of [Redux](https://github.com/reactjs/redux) but over time we added some tweaks to the formula.

ReKotlin is built with Android apps in mind. It is not a full blown client application architecture, instead it focuses only on application state, state transitions and reacting to state changes.

ReKotlin helps you to separate three important concerns of your app's components:

- **State**: The entire application state is explicitly modeled data. This helps avoid complicated state management code, enables better debugging, prevents stateful UI, and has many, many more benefits...
- **State Subscribers**: The application updates when your state changes - be that UI, processing state or networking. The UI layer becomes simple, deterministic rendering of the current application state.
- **State Changes**: The only way to change state is via actions. Actions are small pieces of data that describe a state change. By drastically limiting the way state can be mutated, your app becomes easier to understand and it gets easier to work with many collaborators.

## Core Concepts

ReKotlin builds on a few core concepts:

- The **`Store`** maintains application state. You can only modify the state by dispatching Actions to the store. Whenever the state in the store changes, the store will notify all subscribers.
- An **`Action`** is a declarative way of describing a state change trigger. Actions don't contain any logic, instead reducers consume them to produce the next state.
- A **`Reducer`** is a pure function. It implements the state transition. Based on the incoming action and the current app state the reducer produces the next app state.
- An **`Event`** a declarative way of describing event triggers that don't cause state change.
- A **`Middleware`** intercepts all actions dispatched to the store, before they reach the reducers. This allows you to add capabilities to a store, for example support for asynchronous logic in Thunks, or to handle cross cutting concerns, for example logging.

![](reswift_concept.png)
