package akka.cluster.sbr.strategies.keepreferee

import akka.cluster.sbr.SBSpec
import akka.cluster.sbr.scenarios.{OldestRemovedScenario, SymmetricSplitScenario, UpDisseminationScenario}
import akka.cluster.sbr.utils.PostResolution
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import eu.timepit.refined.scalacheck.all._

class KeepRefereeSpec extends SBSpec {
  "KeepReferee" must {
    "handle symmetric split scenarios" in {
      forAll { (scenario: SymmetricSplitScenario, downAllIfLessThanNodes: Int Refined Positive) =>
        // same referee for everyone
        val referee = scenario.worldViews.head.nodes.head.member.address.toString

        val remainingPartitions = scenario.worldViews.foldMap { worldView =>
          KeepReferee(referee, downAllIfLessThanNodes)
            .takeDecision(worldView)
            .foldMap(PostResolution.fromDecision(worldView))
        }

        remainingPartitions.noSplitBrain shouldBe true
      }
    }

    "handle split during up-dissemination" in {
      forAll { (scenario: UpDisseminationScenario, downAllIfLessThanNodes: Int Refined Positive) =>
        // same referee for everyone
        val referee = scenario.worldViews.head.consideredNodes.take(1).head.member.address.toString

        val remainingSubClusters = scenario.worldViews.foldMap { worldView =>
          KeepReferee(referee, downAllIfLessThanNodes)
            .takeDecision(worldView)
            .foldMap(PostResolution.fromDecision(worldView))
        }

        remainingSubClusters.noSplitBrain shouldBe true
      }
    }

    "handle a split during the oldest-removed scenarios" in {
      forAll { (scenario: OldestRemovedScenario, downAllIfLessThanNodes: Int Refined Positive) =>
        // same referee for everyone
        scenario.worldViews.head.consideredReachableNodes
          .take(1)
          .headOption
          .map { referee =>
            val remainingSubClusters = scenario.worldViews.foldMap { worldView =>
              KeepReferee(referee.member.address.toString, downAllIfLessThanNodes)
                .takeDecision(worldView)
                .foldMap(PostResolution.fromDecision(worldView))
            }

            remainingSubClusters.noSplitBrain shouldBe true
          }
          .getOrElse(succeed)
      }
    }
  }
}
