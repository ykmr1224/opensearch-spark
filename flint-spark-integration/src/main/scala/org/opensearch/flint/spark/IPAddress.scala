/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.flint.spark

import org.apache.spark.sql.types.SQLUserDefinedType

import java.net.{InetAddress, UnknownHostException}

@SQLUserDefinedType(udt = classOf[IPAddressUDT])
case class IPAddress(address: String) extends Ordered[IPAddress] {

  def normalized: String = {
    try {
      InetAddress.getByName(address).getHostAddress
    } catch {
      case _: UnknownHostException => address // fallback
    }
  }

  override def compare(that: IPAddress): Int = {
    this.normalized.compare(that.normalized)
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case other: IPAddress => this.normalized == other.normalized
      case other: String => this.normalized == other
      case _ => false
    }
  }

  override def hashCode(): Int = normalized.hashCode
}
