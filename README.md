
## Actor based Github api crawler

There are 4 kinds of actors in the system, each actor performs a single task:

* **fetcher manager:** work distribution 
* **fetchers:** query the github api
* response interpreter actor
* follower extractor

## Build and test
```bash
sbt
run zhenglaizhang
```

## TODO
1. **Redesign the Actor system** => Akka focus on giving actors as narrow an area of responsibility as possible, gaining better granularity when scaling out and better resilience.