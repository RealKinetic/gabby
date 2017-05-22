## Gabby

Standalone configurable messaging service using a variety of persistence backend.

Hosted multiplexer.

### Reason

#### Standalone
Tunable persistence models that come with at-least once, at-most once, or no guarantees depending on chosen provider.  Different use-cases call for different persistence and latency guarantees.  If streaming, perhaps it's ok to to miss messages.

#### Multiplexer
There are many awesome services out there such as SQS, Pub/Sub, Kinesis, FCM, GCM, SNS but they seem to have one fatal flaw.  For many clients, some form of long-polling is simply the best and easiest option.  The services listed that support these have fairly strict subscription caps.  The goal with gabby is to multiplex services that offer at-least once message delivery using long-polling to ensure they can be used by any number of clients.

FCM and GCM are particularly large disappointments.  Given their use or browser push APIs, messages carrying state in the background interrupt the user experience with annoying popups.  While these services work great with mobile applications, they are unusable for traditional webapps.  Gabby ensures that we can reuse logic in mobile apps and web/thick clients.

We found ourselves writing proxies into Google pub/sub to support long polling.  However, pub/sub has a 10000 subscription limit in an account so which would become problematic with many clients.  We needed a way to multiplex their capabilities to more clients.

We also wanted to make deployment easy.  Gabby is intended to be deployed to AppEngine flex environments which can be done with maven.  The appengine config is found in `src/main/appengine/app.yaml`.

Deployment is as easy as `mvn appengine:deploy`.  That will deploy using the above-stated configuration.

If appengine isn't your thing, this is basically just a spring app and can be deployed standalone anywhere.

### Backends

#### Redis
Redis allows us to make a non-durable out-of-order queuing service with low latencies.  Redis effectively acts as shared memory to instances that can fan out to support large numbers of long-polling instances.

### API Documentation

#### Swagger

Just hit `/swagger-ui.html`.

### TODO

- Add configuration
- Add pub/sub upstream for multiplexing
- Load testing
- SQL downstream to offer durable messaging
- Add more support for memory downstreams
- Better documented swagger.