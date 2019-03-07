package akka.sbr

import akka.actor.Address
import monocle.Getter

/**
 * Represents the strategy that needs to be taken
 * to resolve a potential split-brain issue.
 */
sealed abstract class ResolutionStrategy

object ResolutionStrategy {
  def staticQuorum(quorum: ReachableNodeGroup, unreachableQuorum: UnreachableNodeGroup): ResolutionStrategy =
    (quorum, unreachableQuorum) match {

      /**
       * If we decide DownReachable the entire cluster will shutdown. Always?
       * If we decide DownUnreachable we might create a SB if there's actually quorum in the unreachable
       */
      case (_: ReachableQuorum, _: UnreachablePotentialQuorum) => ???

      /**
       * This side is the quorum, the other side should be downed.
       */
      case (_: ReachableQuorum, subQuorum: UnreachableSubQuorum) => new DownUnreachable(subQuorum) {}

      /**
       * This side is a query and there are no unreachable nodes, nothing needs to be done.
       */
      case (_: ReachableQuorum, _: EmptyUnreachable) => new Idle() {}

      /**
       * Potentially shuts down the cluster if there's
       * no quorum on the other side of the split.
       */
      case (subQuorum: ReachableSubQuorum, _: UnreachablePotentialQuorum) =>
        new DownReachable(subQuorum) {} // TODO create unsafe version?

      /**
       * Both sides are not a quorum.
       *
       * Happens when to many nodes crash at the same time. The cluster will shutdown.
       */
      case (subQuorum: ReachableSubQuorum, _) => new DownReachable(subQuorum) {}
    }

  /**
   * Gets the addresses of the members that should be downed.
   */
  val addressesToDown: Getter[ResolutionStrategy, Set[Address]] = Getter[ResolutionStrategy, Set[Address]] {
    case DownReachable(nodeGroup)   => nodeGroup.nodes.toSortedSet.map(_.address)
    case DownUnreachable(nodeGroup) => nodeGroup.nodes.toSortedSet.map(_.address)
    case _: Idle                    => Set.empty
  }

  implicit class DecisionOps(private val decision: ResolutionStrategy) extends AnyVal {

    /**
     * The addresses of the members that should be downed.
     */
    def addressesToDown: Set[Address] = ResolutionStrategy.addressesToDown.get(decision)
  }
}

/**
 * The reachable nodes should be downed.
 */
sealed abstract case class DownReachable(nodeGroup: ReachableSubQuorum) extends ResolutionStrategy

/**
 * The unreachable nodes should be downed.
 */
sealed abstract case class DownUnreachable(nodeGroup: UnreachableSubQuorum) extends ResolutionStrategy

/**
 * Nothing has to be done. The cluster
 * is in a correct state.
 */
sealed abstract case class Idle() extends ResolutionStrategy