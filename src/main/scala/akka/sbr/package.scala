package akka

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

package object sbr {
  type QuorumSize = Long Refined Positive
}