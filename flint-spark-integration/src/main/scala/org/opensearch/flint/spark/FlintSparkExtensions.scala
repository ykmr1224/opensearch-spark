/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint.spark

import org.opensearch.common.geo.GeoPoint
import org.opensearch.flint.spark.function.TumbleFunction
import org.opensearch.flint.spark.sql.FlintSparkSqlParser
import org.opensearch.flint.spark.udt.{CidrMatch, IPAddress, IPAddressUDT}
import org.opensearch.flint.spark.udt.GeoPointUDT

import org.apache.spark.sql.SparkSessionExtensions
import org.apache.spark.sql.catalyst.FunctionIdentifier
import org.apache.spark.sql.catalyst.expressions.{Expression, ExpressionInfo}
import org.apache.spark.sql.types.UDTRegistration

/**
 * Flint Spark extension entrypoint.
 */
class FlintSparkExtensions extends (SparkSessionExtensions => Unit) {

  override def apply(extensions: SparkSessionExtensions): Unit = {
    extensions.injectParser { (spark, parser) =>
      new FlintSparkSqlParser(parser)
    }

    extensions.injectFunction(TumbleFunction.description)

    extensions.injectOptimizerRule { spark =>
      new FlintSparkOptimizer(spark)
    }

    // Register UDTs
    UDTRegistration.register(classOf[IPAddress].getName, classOf[IPAddressUDT].getName)
    UDTRegistration.register(classOf[GeoPoint].getName, classOf[GeoPointUDT].getName)

    // Register the IPMatch UDF
    val functionName = "cidrmatch"
    val functionClass = classOf[CidrMatch].getName
    val expressionInfo = new ExpressionInfo(functionClass, functionName)
    val cidrmatchBuilder: Seq[Expression] => Expression = {
      case Seq(child1, child2) => CidrMatch(child1, child2)
      case _ => throw new IllegalArgumentException("Invalid arguments for function cidrmatch")
    }
    extensions.injectFunction(
      (FunctionIdentifier(functionName), expressionInfo, cidrmatchBuilder))

    extensions.injectOptimizerRule { spark =>
      OpenSearchCidrMatchConvertRule
    }
  }
}
