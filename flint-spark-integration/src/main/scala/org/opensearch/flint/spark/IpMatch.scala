/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint.spark

import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen._
import org.apache.spark.sql.catalyst.expressions.codegen.Block.BlockHelper
import org.apache.spark.sql.types._

case class IpMatch(left: Expression, right: Expression)
  extends BinaryExpression with Predicate with Serializable {

  override def nullable: Boolean = false
  override def dataType: DataType = BooleanType
  override def prettyName: String = "ip_match"

  override def toString: String = s"ip_match($left, $right)"

  override def nullSafeEval(input1: Any, input2: Any): Any = {
    val ip = input1.asInstanceOf[String]
    val target = input2.asInstanceOf[String]
    ip == target // Just a placeholder; no actual logic needed here
  }

  override protected def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    val leftEval = left.genCode(ctx)
    val rightEval = right.genCode(ctx)
    ev.copy(code =
      code"""
         |${leftEval.code}
         |${rightEval.code}
         |boolean ${ev.value} = ${leftEval.value}.equals(${rightEval.value});
       """.stripMargin, isNull = FalseLiteral)
  }

  override protected def withNewChildrenInternal(newLeft: Expression, newRight: Expression): IpMatch = {
    copy(left = newLeft, right = newRight)
  }
}
