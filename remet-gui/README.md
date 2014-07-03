remet
=====

Real-time metrics in your browser

This is a Play2.1/Java application. Checkout www.playframework.org to get it.

This is the front-end only and needs the tsdaggregator to push the data to it

Remet works by hosting a UI that connects to back-end remet servers.  The UI uses a websockets connection
to each back-end remet server and gets the aggregation data for that server pushed to it.  The UI then uses the
flotr2 library to graph the metrics in real-time.
