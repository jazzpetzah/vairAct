Wire Actors REST Service
====================

REST service, which simulates Wire interaction on a device based on Android Sync Engine actors library (AKKA). The service itself uses
Grizzly HTTP server and is running from source. In theory it can be deployed with any web server, which supports Jersey servlets,
but one didn't manage to compile it properly, since Sync Engine library itself has many transitive dependencies,
which are in conflict with web server libs.

Preparation
-----------

```bash
brew install cputhrottle
```

Usage
-----

The service is currently deplyed on http://192.168.10.44:21080/wire-actors/api/v1. Check DevicesResource.java to see the full list of
available REST methods.

Debug And Deployment
-----

The project can be compiled and executed with Maven
 
 ```bash
 mvn clean compile exec:java 
 ```
 
 JUnit-based unit and integration test templates can be found in src/test subfolder. Make sure you always provide new 
 username for login tests, so it does not violate 8-devices limit. 