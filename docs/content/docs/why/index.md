---
title: "Why ReKotlin?"
date: 2021-05-07T15:18:06+09:00
weight: 3
draft: false
summary: ...but why?
---

## Why ReKotlin?

<!-- TODO: rework this part -->

Model-View-Controller (MVC) is not a holistic application architecture. Typical apps defer a lot of complexity to controllers since MVC doesn't offer other solutions for state management, one of the most complex issues in app development.

Apps built upon MVC often end up with a lot of complexity around state management and propagation. We need to use callbacks, delegations, Key-Value-Observation and notifications to pass information around in our apps and to ensure that all the relevant views have the latest state.

This approach involves a lot of manual steps and is thus error prone and doesn't scale well in complex code bases.

It also leads to code that is difficult to understand at a glance, since dependencies can be hidden deep inside of view controllers. Lastly, you mostly end up with inconsistent code, where each developer uses the state propagation procedure they personally prefer. You can circumvent this issue by style guides and code reviews but you cannot automatically verify the adherence to these guidelines.

ReKotlin attempts to solve these problems by placing strong constraints on the way applications can be written. This reduces the room for programmer error and leads to applications that can be easily understood - by inspecting the application state data structure, the actions and the reducers.

This architecture provides further benefits beyond improving your code base:

- Stores, Reducers, Actions and extensions such as [ReKotlin Router](https://github.com/ReKotlin/rekotlin-router)  are entirely platform independent - you can easily use the same business logic and share it between apps for multiple platforms

The ReKotlin tooling is still in a very early stage, but aforementioned prospects excite us and hopefully others in the community as well!