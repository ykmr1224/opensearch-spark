/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.apache.spark.sql.util

import org.apache.spark.internal.Logging
import org.apache.spark.scheduler.{SparkListener, SparkListenerJobEnd, SparkListenerJobStart, SparkListenerTaskEnd}

class ReadWriteBytesSparkListener(id: String) extends SparkListener with Logging {
  logInfo(s"register SparkListener for ${id}")

  var bytesRead: Long = 0
  var bytesWritten: Long = 0

  override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
    logInfo(s"onJobStart jobId=${jobStart.jobId} time=${jobStart.time}")
    bytesRead = 0
    bytesWritten = 0
  }

  override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit = {
    logInfo(
      s"onTaskEnd taskId=${taskEnd.taskInfo.taskId}, partitionId=${taskEnd.taskInfo.partitionId}")
    val inputMetrics = taskEnd.taskMetrics.inputMetrics
    val outputMetrics = taskEnd.taskMetrics.outputMetrics
    logInfo(s"Bytes read: ${inputMetrics.bytesRead}")
    logInfo(s"Records read: ${inputMetrics.recordsRead}")
    logInfo(s"Bytes written: ${outputMetrics.bytesWritten}")
    logInfo(s"Records written: ${outputMetrics.recordsWritten}")
    bytesRead += inputMetrics.bytesRead
    bytesWritten += outputMetrics.bytesWritten
  }

  override def onJobEnd(jobEnd: SparkListenerJobEnd): Unit = {
    logInfo(s"Total Bytes read: ${bytesRead}")
    logInfo(s"Total Bytes written: ${bytesWritten}")
  }
}
