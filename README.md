<div align="center">
<img width="400" src="docs/logo.png" alt="logo">
</div>

# Nettu scheduler
[![MIT licensed](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build status](https://github.com/fmeringdal/nettu-scheduler/actions/workflows/main.yml/badge.svg)](https://github.com/fmeringdal/nettu-scheduler/actions/workflows/main.yml/badge.svg)
[![codecov](https://codecov.io/gh/fmeringdal/nettu-scheduler/branch/master/graph/badge.svg?token=l5z2mzzdHu)](https://codecov.io/gh/fmeringdal/nettu-scheduler)

## Overview

`Nettu scheduler` is a self-hosted calendar and scheduler server that aims to provide the building blocks for building calendar / booking / appointments apps with ease. It has a simple REST API and also a [JavaScript SDK](https://www.npmjs.com/package/@nettu/sdk-scheduler) and [Rust SDK](https://crates.io/crates/nettu_scheduler_sdk). 

## Features
- **Authentication**: JWT tokens signed by your server for browser clients and api-keys for server to server communication. 
- **Authorization**: JWT tokens have support for attaching policies which defines what actions the subject can take.
- **Booking**: Create a `Service` and register `User`s on it to make them bookable.
- **Calendar Events**: Supports recurrence rules, flexible querying and reminders.
- **Calendars**: For grouping `Calendar Event`s.
- **Metadata queries**: Add key-value metadata to your resources and then query on that metadata 
- **Freebusy**: Find out when `User`s are free and when they are busy.
- **Webhooks**: Notifying your server about `Calendar Event` reminders.

<br/>

<div align="center">
<img src="docs/flow.svg" alt="Application flow">
</div>


## Table of contents

  * [Quick start](#quick-start)
  * [Examples](#examples)
  * [Contributing](#contributing)
  * [License](#license)
  * [Special thanks](#special-thanks)


## Quick start

First of we need a running instance of the server. The quickest way to start one
is with docker:
```bash
docker run -p 5000:5000 fmeringdal/nettu-scheduler
```
or with cargo:
```bash
cd scheduler
cargo run inmemory
```
Both of these methods will start the server with an inmemory data storage which should never
be used in production, but is good enough for exploring what can be done.
For information about setting up this server for deployment, read here.

Now when we have the server running we will need an `Account`. To create an `Account`
we will need the `CREATE_ACCOUNT_SECRET_CODE` which you will find in the server logs
during startup (it can also be set as an environment variable).
```bash
curl -X POST -H "Content-Type: application/json" -d '{"code": "REPLACE_ME"}' http://localhost:5000/accounts
```
The previous command will create an `Account` and the associated `secret_api_key` which is all you need when
your application is going to communicate with the Nettu Scheduler server.

```bash
export SECRET_API_KEY="REPLACE_ME"

# Create a user with metadata
curl -X POST -H "Content-Type: application/json" -H "x-api-key: $SECRET_API_KEY" -d '{"metadata": { "groupId": "123" }}' http://localhost:5000/users

# Get user by metadata
curl -X GET -H "Content-Type: application/json" -H "x-api-key: $SECRET_API_KEY" -d '{"metadata": { "groupId": "123" }}' http://localhost:5000/users/meta
```

Please see below for links to more examples.


## Examples

* [Calendars and Events](#example-1-for-every-7-photos-display-an-ad)

* [Booking](#example-2-for-every-4-paragraphs-of-text-include-2-images)

* [Scheduling](#example-3-in-a-group-of-8-related-links-reserve-positions-5-and-6-for-sponsored-links)

* [Reminders](#example-4-display-a-list-of-songs-including-the-most-successful-songs-for-every-10-songs)

* [Creating JWT for end-users](#example-4-display-a-list-of-songs-including-the-most-successful-songs-for-every-10-songs)


## Contributing

Any contribution or help to this project are always welcome!

## License

[MIT](LICENSE) 

## Special thanks

* [Lemmy](https://github.com/LemmyNet/lemmy) for inspiration on how to use cargo workspace to organize a web app. 
* [The author of this blog post](https://www.lpalmieri.com/posts/2020-09-27-zero-to-production-4-are-we-observable-yet/) for an excellent introduction on how to do telemetry in rust. 