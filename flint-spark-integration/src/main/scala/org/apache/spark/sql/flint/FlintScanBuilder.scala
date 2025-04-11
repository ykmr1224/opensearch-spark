/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.apache.spark.sql.flint

import org.opensearch.flint.spark.skipping.bloomfilter.BloomFilterMightContain
import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.connector.expressions.UserDefinedScalarFunc
import org.apache.spark.sql.connector.expressions.filter.Predicate
import org.apache.spark.sql.connector.read.{Scan, ScanBuilder, SupportsPushDownV2Filters}
import org.apache.spark.sql.flint.config.FlintSparkConf
import org.apache.spark.sql.flint.storage.FlintQueryCompiler
import org.apache.spark.sql.internal.connector.SupportsPushDownCatalystFilters
import org.apache.spark.sql.sources.Filter
import org.apache.spark.sql.types.StructType
import org.opensearch.flint.spark.udt.IpMatch

case class FlintScanBuilder(
    tables: Seq[org.opensearch.flint.core.Table],
    schema: StructType,
    options: FlintSparkConf)
    extends ScanBuilder
    with SupportsPushDownV2Filters
    with Logging {

  private var pushedPredicate = Array.empty[Predicate]
//  private var pushedFilter = Array.empty[Predicate]

  override def build(): Scan = {
    FlintScan(tables, schema, options, pushedPredicate)
  }

  override def pushPredicates(predicates: Array[Predicate]): Array[Predicate] = {
    val (pushed, unSupported) =
      predicates.partition
      {
        case p if isIpMatch(p) => true
//        case p if p.children()(0).name.equalsIgnoreCase("IpMatch") => true
//        case IpMatch(_, _) => true
        case p => FlintQueryCompiler(schema).compile(p).nonEmpty
        case _ => false
      }
    pushedPredicate = pushed
    unSupported
  }

  def isIpMatch(expr: Predicate): Boolean = {
    expr.children.exists {
        case f: UserDefinedScalarFunc if f.name() == "ip_match" => true
        case _ => false
    }
  }

  override def pushedPredicates(): Array[Predicate] = pushedPredicate
    .filterNot(_.name().equalsIgnoreCase(BloomFilterMightContain.NAME))
//
//  override def pushFilters(filters: Seq[Expression]): Seq[Expression] = {
//    val (pushed, unSupported) =
//      filters.partition
//      {
//        case p if p.isInstanceOf[IpMatch] => true
//        case p if FlintQueryCompiler(schema).compileOpt(p).nonEmpty => true
//        case _ => false
//      }
//    pushedPredicate = pushed.map(_.asInstanceOf[Predicate]).toArray
//    unSupported
//  }
//
//  override def pushedFilters: Array[Predicate] = {
//    pushedFilter
//  }
}
