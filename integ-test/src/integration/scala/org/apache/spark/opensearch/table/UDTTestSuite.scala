/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.apache.spark.opensearch.table

import org.opensearch.flint.spark.ppl.FlintPPLSuite
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.UDTRegistration
import org.opensearch.flint.spark.IPAddress

class UDTTestSuite extends OpenSearchCatalogSuite with FlintPPLSuite {
  test("adhoc test") {
    val spark = SparkSession.builder().getOrCreate()

    import spark.implicits._
    spark.udf.register("text_to_ip", (ip: String) => {
      IPAddress(ip)
    })
    spark.udf.register("ip_to_text", (ip: IPAddress) => {
      ip.address
    })

    val data = Seq((1, IPAddress("192.168.0.120")))
    val df = data.toDF("id", "ip")
    df.show()
    df.printSchema()

    df.createOrReplaceTempView("ip_table")

    spark.sql("SELECT * FROM ip_table").show()
    spark.sql("SELECT * FROM ip_table WHERE ip_to_text(ip) = '192.168.0.120'").show()
    spark.sql("SELECT * FROM ip_table WHERE ip = text_to_ip('192.168.0.120')").show()

    // This does not match (no rows returned)
    spark.sql("SELECT * FROM ip_table WHERE ip = text_to_ip('::ffff:192.168.0.120')").show()

    // This fails due to type mismatch
    spark.sql("SELECT * FROM ip_table WHERE ip = '192.168.0.120'").show()
  }
}
