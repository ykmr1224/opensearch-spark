/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint.spark

import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.types._


//implicit val specialStringOrdering: Ordering[String] = Ordering.fromLessThan { (a, b) =>
//  if (a == "special") a.value.length < b.value.length
//  else a.value < b.value
//}
//
object SpecialStringSortRule extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = plan transform {
    case s @ Filter(condition, child) =>
      val schema = child.output
      s
    case s @ Sort(order, global, child) =>
      // Extract schema metadata from child output (i.e., table columns)
      val schema = child.output
      s
//      val newOrdering = order.map { sortOrder =>
//        val columnName = sortOrder.child.asInstanceOf[NamedExpression].name
//        val columnMetadata = schema.find(_.name == columnName).map(_.metadata)
//
//        columnMetadata match {
//          case Some(metadata) if metadata.contains("compareMode") && metadata.getString("compareMode") == "special" =>
//            // Replace standard sorting with custom sorting logic
//            sortOrder.copy(child = SpecialStringOrdering(sortOrder.child, sortOrder.child))
//          case _ =>
//            sortOrder // Keep default sorting
//        }
//      }
//
//      s.copy(order = newOrdering)
  }
}