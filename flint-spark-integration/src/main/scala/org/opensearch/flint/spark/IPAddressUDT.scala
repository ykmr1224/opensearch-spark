/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint.spark

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String

class IPAddressUDT extends UserDefinedType[IPAddress] {

  override def sqlType: DataType = StringType // Store as string in Spark

  override def serialize(obj: IPAddress): UTF8String = UTF8String.fromString(obj.address)

  override def deserialize(datum: Any): IPAddress = datum match {
    case s: UTF8String => IPAddress(s.toString)
    case _ => throw new IllegalArgumentException("Invalid data for IPAddressUDT")
  }

  override def userClass: Class[IPAddress] = classOf[IPAddress]

  override def equals(obj: Any): Boolean = obj match {
    case _: IPAddressUDT => true // All IPAddressUDT instances are the same type
    case _ => false
  }

  override def hashCode(): Int = classOf[IPAddressUDT].hashCode()

  override def asNullable: IPAddressUDT = this
}

case object IPAddressUDT extends IPAddressUDT
