package akka.cluster.sbr.strategies.keepreferee

import akka.cluster.sbr.WorldView
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.Positive

sealed abstract private[keepreferee] class KeepRefereeView extends Product with Serializable

private[keepreferee] object KeepRefereeView {
  def apply(worldView: WorldView, address: String, downAllIfLessThanNodes: Int Refined Positive): KeepRefereeView =
    worldView.reachableConsideredNodes
      .find(_.node.address.toString == address)
      .fold[KeepRefereeView](RefereeUnreachable) { _ =>
        if (worldView.reachableConsideredNodes.size < downAllIfLessThanNodes) TooFewReachableNodes
        else RefereeReachable
      }
}

final private[keepreferee] case object RefereeReachable     extends KeepRefereeView
final private[keepreferee] case object TooFewReachableNodes extends KeepRefereeView
final private[keepreferee] case object RefereeUnreachable   extends KeepRefereeView
