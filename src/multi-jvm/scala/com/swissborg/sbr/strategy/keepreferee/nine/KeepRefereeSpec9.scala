package com.swissborg.sbr.strategy.keepreferee.nine

import akka.remote.transport.ThrottlerTransportAdapter.Direction
import com.swissborg.sbr.TenNodeSpec
import com.swissborg.sbr.strategy.keepreferee.KeepRefereeSpecTenNodeConfig

import scala.concurrent.duration._

class KeepRefereeSpec9MultiJvmNode1 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode2 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode3 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode4 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode5 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode6 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode7 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode8 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode9 extends KeepRefereeSpec9
class KeepRefereeSpec9MultiJvmNode10 extends KeepRefereeSpec9

/**
  * Node8 and node9 are indirectly connected in a ten node cluster
  * Node9 and node10 are indirectly connected in a ten node cluster
  */
class KeepRefereeSpec9 extends TenNodeSpec("KeepReferee", KeepRefereeSpecTenNodeConfig) {
  override def assertions(): Unit =
    "handle scenario 9" in within(120 seconds) {
      runOn(node1) {
        testConductor.blackhole(node8, node9, Direction.Receive).await
        testConductor.blackhole(node9, node10, Direction.Receive).await
      }

      enterBarrier("links-failed")

      runOn(node1, node2, node3, node4, node5, node6, node7) {
        waitForSurvivors(node1, node4, node5, node6, node7)
        waitExistsAllDownOrGone(
          Seq(Seq(node8, node10), Seq(node9))
        )
      }

      enterBarrier("split-brain-resolved")
    }
}
