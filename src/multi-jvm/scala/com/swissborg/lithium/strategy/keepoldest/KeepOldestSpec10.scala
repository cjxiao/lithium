package com.swissborg.lithium

package strategy

package keepoldest

import akka.remote.transport.ThrottlerTransportAdapter.Direction
import com.swissborg.lithium.TestUtil.linksToKillForPartitions

import scala.concurrent.duration._

class KeepOldestSpec10MultiJvmNode1  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode2  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode3  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode4  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode5  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode6  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode7  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode8  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode9  extends KeepOldestSpec10
class KeepOldestSpec10MultiJvmNode10 extends KeepOldestSpec10

/**
 * Network partition between node1 -...- node8 and node9 - node10.
 * Indirect connections between node7 and node8.
 */
sealed abstract class KeepOldestSpec10 extends TenNodeSpec("KeepOldest", KeepOldestSpecTenNodeConfig) {
  override def assertions(): Unit =
    "handle scenario 10" in within(120 seconds) {
      runOn(node1) {
        linksToKillForPartitions(
          List(List(node1, node2, node3, node4, node5, node6, node7, node8), List(node9, node10))
        ).foreach {
          case (from, to) => testConductor.blackhole(from, to, Direction.Both).await
        }

        testConductor.blackhole(node7, node8, Direction.Both).await
      }

      enterBarrier("links-failed")

      runOn(node1, node2, node3, node4, node5, node6) {
        waitForSurvivors(node1, node2, node3, node4, node5, node6)
        waitExistsAllDownOrGone(
          Seq(Seq(node9, node10, node7), Seq(node9, node10, node8))
        )
      }

      runOn(node9, node10) {
        waitForSelfDowning
      }

      enterBarrier("split-brain-resolved")
    }
}
