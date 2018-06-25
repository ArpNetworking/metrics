Metrics
=======

This repository contains scripts for running a full metrics stack for demonstration and end-to-end development.

Most of the other content of this repository has been split among other repositories hosted in the
[ArpNetworking](https://github.com/ArpNetworking) and [Inscope Metrics](https://github.com/InscopeMetrics) Github
organizations.

Demo
----

The `demo` directory contains scripts for running a fully encapsulated end-to-end environment for demonstration
purposes. All components are executed from binaries within a Vagrant controlled virtual machine. During provisioning
the _latest_ versions of Metrics Aggregator Daemon, Cluster Aggregator and Metrics Portal will be installed. If you
wish to update these components in your demo environment, simply destroy the environment with `vagrant destroy` and
then launch again.

To launch the stack execute:
```
demo> ./start.sh
```

To terminate the stack simply press `Ctrl+c`.

Options for `start.sh` are:

* `-p` -- run pinger
* `-v` -- verbose mode

The components are available at the following addresses:

* KairosDb: [localhost:8082](http://localhost:8082)
* Grafana: [localhost:8081](http://localhost:8081)
* Metrics Portal: [localhost:8080](http://localhost:8080)

Build
-----

The `build` directory contains scripts for running an end-to-end development environment for development of our
components: Metrics Aggregator Daemon, Cluster Aggregator Daemon and Metrics Portal. These development components are
executed from source code directories expected as siblings to the metrics project directory (e.g. `git clone` into the
same parent directory). The other three components, [Syclla](https://www.scylladb.com/open-source/) (Cassandra
equivalent), [KairosDb](https://kairosdb.github.io/) and [Grafana](https://grafana.com/) are executed from binaries
within a Vagrant controlled virtual machine.

To launch the stack execute:
```
build> ./start.sh -a
```

To terminate the stack simply press `Ctrl+c`.

Options for `start.sh` are:

* `-s service` -- specify a service to start; valid services are: mad, cagg, mportal, ckg
* `-a` -- start all services
* `-c` -- clear logs
* `-p` -- run pinger
* `-n` -- don't build
* `-v` -- verbose mode

The components are available at the following addresses:

* KairosDb: [localhost:8082](http://localhost:8082)
* Grafana: [localhost:8081](http://localhost:8081)
* Metrics Portal: [localhost:8080](http://localhost:8080)
* Metrics Aggregator Daemon: [localhost:7090](http://localhost:7090/ping)
* Cluster Aggregator: [localhost:7066](http://localhost:7066/ping)

Component configuration is available in `build/config/<component>` and is automatically reloaded by Metrics Aggregator
Daemon and Cluster Aggregator.

If you are iterating on one component of the stack, you should run `start.sh` twice to avoid restarting the entire
stack with each change. For example, if you are iterating on Cluster Aggregator then start the rest of the stack first:

```
build> ./start.sh -s mad -s mportal -s ckg
```

Then start Cluster Aggregator:

```
build> ./start.sh -s cagg
```

Then after you make changes to Cluster Aggregator just shutdown the second `start.sh` process and restart it. By default
`start.sh` will even ensure the project has been built! Of course, you can disable this behavior by adding the `-n`
argument.

Logs for development each component are output to the `logs` directory in that component's project directory. The
configuration for component logging is controlled from this project via the files in `config/<component>/logback.xml`.

Similarly, configuration of each component is found in `config/<component>/`.

Finally, all three development components are started with remote debugging enabled. The ports for each service are:

* Metrics Aggregator Daemon - 9001
* Cluster Aggregator - 9002
* Metrics Portal - 9003

Prerequisites
-------------
* [Vagrant](https://www.vagrantup.com/)
* [Virtual Box](https://www.virtualbox.org/)

FAQ
---

Q: _What is the username and password to Grafana?_
A: The default username and password are both `admin`.

Q: _How do I resolve the error: `Vagrant was unable to mount VirtualBox shared folders.`?
A: You need to install the Virtual Box Guest extension for Vagrant by running `vagrant plugin install vagrant-vbguest` from the `demo` or `build` directory.

Q. _Why does my dashboard json creation fail with `"fieldNames":["Dashboard"],"classification":"RequiredError"`?_
A. You need to wrap the dashboard json from the Grafana user interface with the following JSON:
```
{
  "overwrite": true,
  "dashboard": <JSON FROM UI HERE>
}
```

Q. _Why does my dashboard json creation fail with `"Dashboard not found","status":"not-found"`?_
A. You need to remove the top-level `"id":<INT>` field from the dashboard json from the Grafana user interface.

License
-------

Published under Apache Software License 2.0, see [LICENSE](LICENSE)

&copy; Inscope Metrics Inc., 2016
