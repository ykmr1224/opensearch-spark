/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint.spark

import java.util.Base64

import com.stephenn.scalatest.jsonassert.JsonMatchers.matchJson
import org.opensearch.flint.common.FlintVersion.current
import org.opensearch.flint.core.storage.FlintOpenSearchIndexMetadataService
import org.opensearch.flint.spark.covering.FlintSparkCoveringIndex.getFlintIndexName
import org.scalatest.matchers.must.Matchers.defined
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import org.apache.spark.sql.Row
import org.apache.spark.sql.flint.config.FlintSparkConf

class FlintSparkCoveringIndexITSuite extends FlintSparkSuite {

  /** Test table and index name */
  private val testTable = "spark_catalog.default.ci_test"
  private val testIndex = "name_and_age"
  private val testFlintIndex = getFlintIndexName(testIndex, testTable)
  private val testLatestId = Base64.getEncoder.encodeToString(testFlintIndex.getBytes)

  override def beforeAll(): Unit = {
    super.beforeAll()

    createPartitionedAddressTable(testTable)
  }

  override def afterEach(): Unit = {
    super.afterEach()

    // Delete all test indices
    deleteTestIndex(testFlintIndex)
  }

  test("create covering index with metadata successfully") {
    flint
      .coveringIndex()
      .name(testIndex)
      .onTable(testTable)
      .addIndexColumns("name", "age")
      .filterBy("age > 30")
      .create()

    val index = flint.describeIndex(testFlintIndex)
    index shouldBe defined
    FlintOpenSearchIndexMetadataService.serialize(index.get.metadata()) should matchJson(s"""{
         |   "_meta": {
         |     "version": "${current()}",
         |     "name": "name_and_age",
         |     "kind": "covering",
         |     "indexedColumns": [
         |     {
         |        "columnName": "name",
         |        "columnType": "string"
         |     },
         |     {
         |        "columnName": "age",
         |        "columnType": "int"
         |     }],
         |     "source": "spark_catalog.default.ci_test",
         |     "options": {
         |       "auto_refresh": "false",
         |       "incremental_refresh": "false"
         |     },
         |     "latestId": "$testLatestId",
         |     "properties": {
         |       "filterCondition": "age > 30"
         |     }
         |   },
         |   "properties": {
         |     "name": {
         |       "type": "keyword"
         |     },
         |     "age": {
         |       "type": "integer"
         |     }
         |   }
         | }
         |""".stripMargin)
  }

  test("full refresh covering index successfully") {
    flint
      .coveringIndex()
      .name(testIndex)
      .onTable(testTable)
      .addIndexColumns("name", "age")
      .create()

    flint.refreshIndex(testFlintIndex)

    val indexData = flint.queryIndex(testFlintIndex)
    checkAnswer(indexData, Seq(Row("Hello", 30), Row("World", 25)))
  }

  test("incremental refresh covering index successfully") {
    flint
      .coveringIndex()
      .name(testIndex)
      .onTable(testTable)
      .addIndexColumns("name", "age")
      .options(FlintSparkIndexOptions(Map("auto_refresh" -> "true")), testIndex)
      .create()

    val jobId = flint.refreshIndex(testFlintIndex)
    jobId shouldBe defined

    val job = spark.streams.get(jobId.get)
    failAfter(streamingTimeout) {
      job.processAllAvailable()
    }

    val indexData = flint.queryIndex(testFlintIndex)
//    checkAnswer(indexData, Seq(Row("Hello", 30), Row("World", 25)))

    val indexOptions = flint.describeIndex(testFlintIndex)
    indexOptions shouldBe defined
    indexOptions.get.options.checkpointLocation() shouldBe None
  }

  test("create covering index with default checkpoint location successfully") {
    withTempDir { checkpointDir =>
      conf.setConfString(
        FlintSparkConf.CHECKPOINT_LOCATION_ROOT_DIR.key,
        checkpointDir.getAbsolutePath)
      flint
        .coveringIndex()
        .name(testIndex)
        .onTable(testTable)
        .addIndexColumns("name", "age")
        .options(FlintSparkIndexOptions(Map("auto_refresh" -> "true")), testFlintIndex)
        .create()

      val jobId = flint.refreshIndex(testFlintIndex)
      jobId shouldBe defined

      val job = spark.streams.get(jobId.get)
      failAfter(streamingTimeout) {
        job.processAllAvailable()
      }

      val indexData = flint.queryIndex(testFlintIndex)
//      checkAnswer(indexData, Seq(Row("Hello", 30), Row("World", 25)))

      val index = flint.describeIndex(testFlintIndex)
      index shouldBe defined

      val checkpointLocation = index.get.options.checkpointLocation()
      assert(checkpointLocation.isDefined, "Checkpoint location should be defined")
      assert(
        checkpointLocation.get.contains(testFlintIndex),
        s"Checkpoint location dir should contain ${testFlintIndex}")

      conf.unsetConf(FlintSparkConf.CHECKPOINT_LOCATION_ROOT_DIR.key)
    }
  }

  test("auto refresh covering index successfully with external scheduler") {
    withTempDir { checkpointDir =>
      flint
        .coveringIndex()
        .name(testIndex)
        .onTable(testTable)
        .addIndexColumns("name", "age")
        .options(
          FlintSparkIndexOptions(
            Map(
              "auto_refresh" -> "true",
              "scheduler_mode" -> "external",
              "checkpoint_location" -> checkpointDir.getAbsolutePath)),
          testIndex)
        .create()

      val jobId = flint.refreshIndex(testFlintIndex)
      jobId shouldBe None

      val indexData = flint.queryIndex(testFlintIndex)
//      checkAnswer(indexData, Seq(Row("Hello", 30), Row("World", 25)))
    }
  }

  test("update covering index successfully") {
    // Create full refresh Flint index
    flint
      .coveringIndex()
      .name(testIndex)
      .onTable(testTable)
      .addIndexColumns("name", "age")
      .create()
    val indexData = flint.queryIndex(testFlintIndex)
//    checkAnswer(indexData, Seq())

    // Update Flint index to auto refresh and wait for complete
    val updatedIndex = flint
      .coveringIndex()
      .copyWithUpdate(
        flint.describeIndex(testFlintIndex).get,
        FlintSparkIndexOptions(Map("auto_refresh" -> "true")))
    val jobId = flint.updateIndex(updatedIndex)
    jobId shouldBe defined

    val job = spark.streams.get(jobId.get)
    failAfter(streamingTimeout) {
      job.processAllAvailable()
    }

//    checkAnswer(indexData, Seq(Row("Hello", 30), Row("World", 25)))
  }

  test("can have multiple covering indexes on a table") {
    flint
      .coveringIndex()
      .name(testIndex)
      .onTable(testTable)
      .addIndexColumns("name", "age")
      .create()

    val newIndex = testIndex + "_address"
    flint
      .coveringIndex()
      .name(newIndex)
      .onTable(testTable)
      .addIndexColumns("address")
      .create()
    deleteTestIndex(getFlintIndexName(newIndex, testTable))
  }
}
