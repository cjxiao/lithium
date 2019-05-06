package akka.cluster.sbr

import akka.actor.{Actor, ActorLogging, Address, Props}
import akka.cluster.Cluster
import akka.cluster.sbr.strategies.Or
import akka.cluster.sbr.strategies.indirected.Indirected
import akka.cluster.sbr.strategy.Strategy
import cats.data.OptionT
import cats.effect.SyncIO
import cats.implicits._

import scala.concurrent.duration._

class Downer(cluster: Cluster, strategy: Strategy, stableAfter: FiniteDuration, downAllWhenUnstable: FiniteDuration)
    extends Actor
    with ActorLogging {
  import Downer._

  private val selfAddress = cluster.selfMember.address
  private val indirected  = new Indirected
  private val _           = context.system.actorOf(StabilityReporter.props(self, stableAfter, downAllWhenUnstable, cluster))

  override def receive: Receive = {
    case h @ HandleSplitBrain(worldView) =>
      log.debug("{}", h)

      Or(strategy, indirected)
        .takeDecision(worldView)
        .map(execute)
        .toTry
        .get
        .unsafeRunSync()
  }

  private def execute(decision: StrategyDecision): SyncIO[Unit] =
    leader
      .map(_ == selfAddress)
      .ifM(down(decision.nodesToDown) >> liftSyncIO(log.debug("Executing decision: {}", decision.clean)), syncIOUnit)
      .value
      .void

  private val leader: OptionT[SyncIO, Address] = OptionT(SyncIO(cluster.state.leader))

  private def down(nodes: Set[Node]): OptionT[SyncIO, Unit] =
    OptionT.liftF(nodes.toList.traverse_(node => SyncIO(cluster.down(node.member.address))))

}

object Downer {
  def props(cluster: Cluster,
            strategy: Strategy,
            stableAfter: FiniteDuration,
            downAllWhenUnstable: FiniteDuration): Props =
    Props(new Downer(cluster, strategy, stableAfter, downAllWhenUnstable))

  final case class HandleSplitBrain(worldView: WorldView)

  def liftSyncIO[A](f: => A): OptionT[SyncIO, A] = OptionT.liftF(SyncIO(f))
  val syncIOUnit: OptionT[SyncIO, Unit]          = OptionT.pure(())
}
