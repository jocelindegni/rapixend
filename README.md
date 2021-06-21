# RAPIDXEND

### A scalable file transfer based-on `Spring-boot`, `Mongodb`, `Redis pub/sub` 
Rapidxend is an open source project allowing to deploy in a distributed system, a file sharing system.

## Features provided
- Send files to multiple devices simultaneously
- Resume transfer after an incident (such as a loss of connection)
- Distributed architecture (improve transfer speed by adding new instances)

# Installation and configuration

### Pre-requisites
- `Java 8` or later
- `IDE` with lombaok configuration (Check https://www.baeldung.com/lombok-ide)
- `Docker environment`

### Clone project
```
$ git clone https://github.com/jocelindegni/rapixend.git
```

### Run applicattion

Before run the application, some environement variables are required

-`MONGO.connection_url` : The MONGO connection url. Default value is `mongodb//localhost:27017`

-`MONGO.db_name` : The MONGO database name. Default value is `rapidxend`

-`REDIS.host` : The REDIS hostname or IP address. The default value is `localhost`

-`REDIS.port` : The REDIS port. Default value is `6379`

-`REDIS.channel` : The redis channel to publish and subcribe it. Default value is `notification`

```
$ cd rapixend

$ mvn spring-boot:run -DREDIS.channel=event -DMONGO.db_name=db
```

# Technologies

- `Spring mongo` For data storage
- `Spring websocket` for notifying devices in real-time
- `Spring redis messaging` For communication between instances
- `Testcontainers` Allow to run docker instance into java

# What's next ?
- Improve the transfer algorithm (e.g. send the data blocks in a random order).
- Add a kubernetes configuration for automatic deployment


# LICENSE

MIT LICENSE

Copyright © 2021 Jocelin DEGNI
