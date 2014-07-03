Query Log Format
================

Provides a platform for recording, aggregating, publishing and accessing metrics produced by your host system, shared resource, or application. 


Versions
--------

### 2G *(Proposed)*

* *Example Not Available*
* *Schema Not Available*

Changes:
* Include time offset relative to initTimestamp on each timer sample.

### 2F *(Proposed)*

* *Example Not Available*
* *Schema Not Available*

Changes:
* Support for compound units (e.g. Megabytes per second).
* Fully self-describing:
    * Include service name
    * Include cluster name
    * Include host name

### 2E

* [Example](query-log-example-2e.json)
* [Schema](query-log-schema-2e.json)

Changes:
* Wrap the metrics payload in a formatted container to better support general log routing and storage.
    * The time, name and level fields must be serialized first and in that order.
    * The time must be in ISO8601 with date, time and timezone.

### 2D

* [Example](query-log-example-2d.json)
* [Schema](query-log-schema-2d.json)

Changes:
* Expand counter, timer and gauge samples to include optional units.
* Switch finalTimestamp and initTimestamp annotations from epoch to ISO8601.

### 2C

* [Example](query-log-example-2c.json)
* [Schema](query-log-schema-2c.json)

Changes:
* First standardized JSON format.

### Pre-2C

All versions prior to 2C of the file format and are considered *deprecated*.
