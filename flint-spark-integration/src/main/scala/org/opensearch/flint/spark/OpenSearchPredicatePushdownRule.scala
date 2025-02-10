/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint.spark

import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.datasources.LogicalRelation

object OpenSearchPredicatePushdownRule extends Rule[LogicalPlan] {
  override def apply(plan: LogicalPlan): LogicalPlan = plan transform {
    case Filter(condition, relation@LocalRelation(_, _, _)) =>
      val newCondition = condition transform {
        case IpMatch(left, right) =>
          // Translate to OpenSearch Query
          val field = left.asInstanceOf[Attribute].name
          val value = right.asInstanceOf[Literal].value.toString
          val query = s"""{"term": {"$field": "$value"}}"""

//          relation.copy(relation = osRelation)
          // Attach the OpenSearch query as a hint
//          relation.copy(relation = osRelation.copy(parameters =
//            osRelation.parameters + ("es.query" -> query))
//          )
          condition
      }
      Filter(condition, relation)
  }
}