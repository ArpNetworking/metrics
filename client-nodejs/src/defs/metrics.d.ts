/*
 * Copyright 2014 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This module serves two purposes:
 *   1. Act as reference for the public interface the library is exposing.
 *   2. Enforce compile type checking by Typescript compiler.
 *
 * Please note none of this code gets actually emitted by the compiler, so you shall
 * not find any traces of this file in the Javascript code.
 */
declare module "tsdDef" {
    /**
     * Units available for recording metrics. The units are used to aggregate values
     * of the same metric published in different units (e.g. bytes and kilobytes).
     * Publishing a metric with units from different domains will cause some of the
     * data to be discarded by the aggregator (e.g. bytes and seconds). This
     * includes discarding data when some data has a unit and some data does not
     * have any unit. This library cannot detect such inconsistencies since
     * aggregation can occur across Metric instances, processes and even hosts.
     *
     * This is a forward decalration of the Unit interface. Predefined values will be
     * implemented in the 'Units' class
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    export interface Unit {
        name:string;
    }

    /**
     * Interface for logging metrics: timers, counters and gauges for TSD Aggregator
     *
     * @author Mohammed Kamel (mkamel at groupon dot com)
     *
     */
    export interface Metrics {
        /**
         * Create and initialize a counter sample. It is valid to create multiple
         * <code>Counter</code> instances with the same name, even concurrently,
         * each will record a unique sample for the counter of the specified name.
         *
         * @param name The name of the counter.
         * @return <code>Counter</code> instance for recording a counter sample.
         */
        createCounter(name:string): Counter;

        /**
         * Increment the specified counter by 1. All counters are initialized to
         * zero.
         *
         * @param name The name of the counter.
         */
        incrementCounter(name:string): void;

        /**
         * Increment the specified counter by the specified amount. All counters are
         * initialized to zero.
         *
         * @param name The name of the counter.
         * @param value The amount to increment by.
         */
        incrementCounter(name:string, value:number): void ;

        /**
         * Decrement the specified counter by 1. All counters are initialized to
         * zero.
         *
         * @param name The name of the counter.
         */
        decrementCounter(name:string): void;

        /**
         * Decrement the specified counter by the specified amount. All counters are
         * initialized to zero.
         *
         * @param name The name of the counter.
         * @param value The amount to decrement by.
         */
        decrementCounter(name:string, value:number): void ;

        /**
         * Reset the counter to zero. This most commonly used to record a zero-count
         * for a particular counter. If clients wish to record set count metrics
         * then all counters should be reset before conditionally invoking increment
         * and/or decrement.
         *
         * @param name The name of the counter.
         */
        resetCounter(name:string): void;

        /**
         * Create and start a timer. It is valid to create multiple <code>Timer</code>
         * instances with the same name, even concurrently, each will record a
         * unique sample for the timer of the specified name.
         *
         * @param name The name of the timer.
         * @return <code>Timer</code> instance for recording a timing sample.
         */
        createTimer(name:string): Timer;

        /**
         * Start the specified timer measurement.
         *
         * @param name The name of the timer.
         */
        startTimer(name:string): void;

        /**
         * Stop the specified timer measurement.
         *
         * @param name The name of the timer.
         */
        stopTimer(name:string): void;

        /**
         * Set the timer to the specified value. This is most commonly used to
         * record timers from external sources that are not integrated with metrics.
         *
         * @param name The name of the timer.
         * @param duration The duration of the timer
         * @param unit     The unit of time of the duration.
         */
        setTimer(name:string, duration:number, unit:Unit): void;

        /**
         * Set the specified gauge reading.
         *
         * @param name The name of the gauge.
         * @param value The reading on the gauge
         * @param unit The unit of time of the reading.
         */
        setGauge(name:string, value:number, unit:Unit): void;

        /**
         * Add an attribute that describes the captured metrics or context.
         *
         * @param key The name of the attribute.
         * @param value The value of the attribute.
         */
        annotate(key:string, value:string): void;

        /**
         * Close the metrics object. This should complete publication of metrics to
         * the underlying data store. Once the metrics object is closed, no further
         * metrics can be recorded.
         */
        close(): void;

        /**
         * Accessor to determine if this <code>Metrics</code> instance is open or
         * closed. Once closed an instance will not record new data.
         *
         * @return True if and only if this <code>Metrics</code> instance is open.
         */
        isOpen(): boolean;
    }

    /**
     * Interface for timer. Instances are started on creation and record state when
     * stopped or closed.
     *
     * Each timer instance is bound to a <code>Metrics</code> instance. After the
     * <code>Metrics</code> instance is closed any timing data generated by
     * <code>Timer</code> instances bound to that <code>Metrics</code> instance will
     * be discarded.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    export interface Timer extends MetricSample {

        /**
         * Stop the timer and record timing data in the associated.
         * <code>Metrics</code> instance.
         */
        stop(): void;

        /**
         * Return if the timer was stopped already.
         */
        isStopped(): boolean;
    }

    /**
     * Interface for counter. Instances are initialized to zero on creation. The
     * zero-value sample is recorded when the <code>Metrics</code> instance is
     * closed if no other actions are taken on the <code>Counter</code>.
     *
     * Modifying the <code>Counter</code> instance's value modifies the single
     * sample value. When the associated <code>Metrics</code> instance is closed
     * whatever value the sample has is recorded. To create another sample you
     * create a new <code>Counter</code> instance with the same name.
     *
     * Each counter instance is bound to a <code>Metrics</code> instance. After the
     * <code>Metrics</code> instance is closed any modifications to the
     * <code>Counter</code> instances bound to that <code>Metrics</code> instance
     * will be ignored.
     *
     * @author Ville Koskela (vkoskela at groupon dot com)
     */
    export interface Counter extends MetricSample {

        /**
         * Increment the counter sample by 1.
         */
        increment(): void;

        /**
         * Decrement the counter sample by 1.
         */
        decrement(): void;

        /**
         * Increment the counter sample by the specified value.
         *
         * @param value The value to increment the counter by.
         */
        increment(value:number): void ;

        /**
         * Decrement the counter sample by the specified value.
         *
         * @param value The value to decrement the counter by.
         */
        decrement(value:number): void;
    }

    /**
     * Interface for value and unit. This class is thread safe.
     *
     * @author Mohammed Kamel (mkamel at groupon dot com)
     */
    export interface MetricSample {

        /**
         * Access the magnitude of the quantity.
         *
         * @return The magnitude.
         */
        getValue(): number;

        /**
         * Access the units that the magnitude is expressed in.
         *
         * @return The units of the magnitude.
         */
        getUnit(): Unit;
    }

    /**
     *
     */
    export interface MetricsList<T extends MetricSample> {

        /**
         * An array holding metrics samples.
         *
         * @method
         * @returns {T[]}
         */
        getValues():T[];

        /**
         * Push a value to the list.
         *
         * @method
         * @param {T} value The value to be pushed
         */
        push(value:T):void;

        /**
         * Creates a new MetricsList with all elements that pass the test implemented by the provided predicate.
         *
         * @method
         * @param predicate The function to test if element should be taken.
         * @return {MetricsList<T>} new MetricsList list containing only the items matching the filter predicate
         */
        filter(predicate:(sample:T) => boolean):MetricsList<T>;
    }

    /**
     * An interface for an instance holding all the recorded metrics to be serialized.
     */
    export interface MetricsEvent {
        /**
         * The annotations represented as hash of arrays indexed by annotation name.
         * @type {{Object.<string, MetricsList>}}
         */
        annotations:{[name:string]: string};

        /**
         * Counters and their samples recorded represented as hash of counter name to
         * [MetricSample]{@linkcode MetricSample}
         * @type {{Object.<string, MetricsList>}}
         */
        counters:{[name:string]: MetricsList<MetricSample>};

        /**
         * Gauges and their samples recorded represented as hash of counter name to
         * [MetricSample]{@linkcode MetricSample}
         * @type {{Object.<string, MetricsList>}}
         */
        gauges:{[name:string]: MetricsList<MetricSample>};

        /**
         * Timers and their samples recorded represented as hash of counter name to
         * [MetricSample]{@linkcode MetricSample}
         * @type {{Object.<string, MetricsList>}}
         */
        timers:{[name:string]: MetricsList<MetricSample>};
    }

    /**
     * Class for holding a list of samples.
     */
    export interface MetricsList<T extends MetricSample> {
        /**
         * Push a value to the list.
         *
         * @method
         * @param {T} value The value to be pushed
         */
        push(value:T):void;

        /**
         * Creates a new MetricsList with all elements that pass the test implemented by the provided predicate.
         *
         * @method
         * @param predicate The function to test if element should be taken.
         * @return {MetricsList<T>} new MetricsList list containing only the items matching the filter predicate
         */
        filter(predicate:(T) => boolean):MetricsList<T>;
    }

    /**
     * Interface representing a destination to record metrics to.
     *
     * @author Mohammed Kamel (mkamel at groupon dot com)
     */
    export interface Sink {

        /**
         * Invoked by <code>Metrics</code> to record data to this <code>Sink</code>.
         *
         * @param metricsEvent metrics event to be recorded by the sync.
         */
        record(metricsEvent:MetricsEvent): void;
    }
}
