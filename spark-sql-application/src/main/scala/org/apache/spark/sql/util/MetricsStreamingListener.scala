/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.apache.spark.sql.util

import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.codahale.metrics.{MetricRegistry, Timer}
import org.opensearch.flint.core.metrics.{MetricConstants, MetricsUtil}

import org.apache.spark.internal.Logging
import org.apache.spark.sql.streaming.{StreamingQuery, StreamingQueryListener}
import org.apache.spark.sql.streaming.StreamingQueryListener.{QueryProgressEvent, QueryStartedEvent, QueryTerminatedEvent}
import org.apache.spark.streaming.scheduler.{StreamingListener, StreamingListenerBatchCompleted, StreamingListenerBatchStarted, StreamingListenerOutputOperationStarted, StreamingListenerReceiverStarted, StreamingListenerReceiverStopped}

class MetricsStreamingListener extends StreamingQueryListener with Logging {
  logInfo(s"MetricsStreamingListener")

  override def onQueryStarted(event: QueryStartedEvent): Unit = {
    logInfo(s"onQueryStarted")
  }

  override def onQueryProgress(event: QueryProgressEvent): Unit = {
    MetricsUtil.incrementCounter(MetricConstants.STREAMING_REFRESH_SUCCESS_METRIC);
    MetricsUtil
      .getTimer(MetricConstants.STREAMING_REFRESH_PROCESSING_TIME_METRIC)
      .update(event.progress.batchDuration, TimeUnit.MILLISECONDS);
    MetricsUtil
      .getTimer("test.processingTime")
      .update(Duration.ofNanos(12345678L));
    MetricsUtil
      .getTimer("test.processingTime")
      .update(Duration.ofMillis(12345L));
    MetricsUtil.getMetricRegistry().getMetrics.forEach { (k, v) =>
      logInfo(s"$k : $v")
    }
    logInfo(s"onQueryProgress batchDuration=${event.progress.batchDuration}ms")
  }

  override def onQueryTerminated(event: QueryTerminatedEvent): Unit = {
    MetricsUtil.incrementCounter(MetricConstants.STREAMING_REFRESH_FAILED_METRIC);
    logInfo(s"onQueryTerminated")
  }
}
