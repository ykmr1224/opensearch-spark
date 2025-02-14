/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.apache.spark.opensearch.table

import org.apache.spark.{SparkConf, SparkContext}
import org.opensearch.flint.spark.{FlintPPLSparkExtensions, FlintSparkExtensions, IPAddress, IPAddressUDT}
import org.opensearch.flint.spark.ppl.FlintPPLSuite
import org.apache.spark.sql.{Row, SparkSession, SparkSessionExtensions}
import org.apache.spark.sql.catalyst.analysis.{Analyzer, TypeCoercion}
import org.apache.spark.sql.catalyst.expressions.{Cast, Expression}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.flint.config.FlintSparkConf.OPTIMIZER_RULE_ENABLED
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.UDTRegistration


case class CustomTypeCoercionRule(spark: SparkSession) extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = plan transform {
    case p: LogicalPlan => {
        //scalastyle:off println
        println(s"CustomTypeCoercionRule.apply")
        p
      }
//    case c @ Cast(child, StringType, _) if child.dataType == StringType =>
//      Cast(child, IPAddressUDT, None)
  }
}

//class CustomSparkExtensions extends (SparkSessionExtensions => Unit) {
//  override def apply(extensions: SparkSessionExtensions): Unit = {
//    //scalastyle:off println
//    println(s"CustomSparkExtensions.apply")
////    // Parser extension
////    extensions.injectParser { (spark, parser) =>
////      new CustomSqlParser(parser)
////    }
////
////    // Function extension
////    extensions.injectFunction(CustomFunction.description)
////
////    // Optimizer rule extension
////    extensions.injectOptimizerRule { spark =>
////      new CustomOptimizerRule(spark)
////    }
//
//    extensions.injectResolutionRule { spark =>
//      CustomTypeCoercionRule(spark)
//    }
//
//    extensions.injectOptimizerRule { spark =>
//      CustomTypeCoercionRule(spark)
//    }
//
////    // Planner strategy extension
////    extensions.injectPlannerStrategy { spark =>
////      new CustomPlannerStrategy(spark)
////    }
//  }
//}


class UDTTestSuite extends OpenSearchCatalogSuite with FlintPPLSuite {
  test("adhoc test") {
    // setup spark session with CustomSparkExtensions
    val spark = SparkSession.builder()
//      .config("spark.sql.extensions", classOf[CustomSparkExtensions].getName)
      .getOrCreate()

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

//    testSql("SELECT * FROM ip_table")
//    testSql("SELECT * FROM ip_table WHERE ip_to_text(ip) = '192.168.0.120'")
    testSql("SELECT * FROM ip_table WHERE ip = text_to_ip('192.168.0.120')")
//
//    // This does not match (no rows returned)
//    testSql("SELECT * FROM ip_table WHERE ip = text_to_ip('::ffff:192.168.0.120')")

    // This fails due to type mismatch
//    testSql("SELECT * FROM ip_table WHERE 1.0 = '1' and ip = '192.168.0.120'")
  }

  def testSql(sql: String) = {
    val df = spark.sql(sql)
    df.explain(true)
    df.show()
    df.printSchema()
  }
}
