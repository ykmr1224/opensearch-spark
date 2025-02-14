/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.{Cast, EqualNullSafe, EqualTo, Expression}
import org.apache.spark.sql.catalyst.plans.logical.{Filter, LogicalPlan}
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.types.StringType

case class CustomTypeCoercionRule(spark: SparkSession) extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = {
    plan.transformUpWithBeforeAndAfterRuleOnChildren (!_.analyzed) {
      case (beforeMapChildren, afterMapChildren) =>
        if (!afterMapChildren.childrenResolved) {
          afterMapChildren
        } else {
          beforeMapChildren
//          // Only propagate types if the children have changed.
//          val withPropagatedTypes = if (beforeMapChildren ne afterMapChildren) {
//            propagateTypes(afterMapChildren)
//          } else {
//            beforeMapChildren
//          }
//          withPropagatedTypes.transformExpressionsUpWithPruning(
//            AlwaysProcess.fn, ruleId)(typeCoercionFn)
        }
//      case c @ Cast(child, StringType, _, _) if child.dataType == StringType =>
//        //scalastyle:off println
//        println(s"CustomTypeCoercionRule.apply")
      //    case f @ Filter(condition, child) =>
      //      val newCondition = condition transform {
      //        case EqualTo(left, right) if needsImplicitCast(left, right) =>
      //          castToMatch(left, right)
      //        case EqualNullSafe(left, right) if needsImplicitCast(left, right) =>
      //          castToMatch(left, right)
      //      }
      //      Filter(newCondition, child)
      //scalastyle:off println
      //      println(s"CustomTypeCoercionRule.apply")
      //      f.copy(child = apply(child))
      //    case c @ Cast(child, StringType, _, _) if child.dataType == StringType =>
      //      //scalastyle:off println
      //      println(s"CustomTypeCoercionRule.apply")
      //      c
      //    case p: LogicalPlan =>
      //      //scalastyle:off println
      //      println(s"CustomTypeCoercionRule.apply")
      //      p
      //    case c @ Cast(child, StringType, _) if child.dataType == StringType =>
      //      Cast(child, IPAddressUDT, None)
    }
  }

  private def needsImplicitCast(left: Expression, right: Expression): Boolean = {
    (left.dataType, right.dataType) match {
      case (IPAddressUDT, StringType) | (StringType, IPAddressUDT) => true
      case _ => false
    }
  }

  /** Apply Cast to convert String to IPAddressUDT */
  private def castToMatch(left: Expression, right: Expression): Expression = {
    (left.dataType, right.dataType) match {
      case (IPAddressUDT, StringType) => EqualTo(left, Cast(right, IPAddressUDT))
      case (StringType, IPAddressUDT) => EqualTo(Cast(left, IPAddressUDT), right)
      case _ => EqualTo(left, right)  // Fallback, should not happen
    }
  }
}
