package com.swissborg.sbr.strategy.keepmajority

import com.swissborg.sbr.FiveNodeSpecConfig
import com.typesafe.config.ConfigFactory

object RoleKeepMajoritySpecConfig
    extends FiveNodeSpecConfig("keepmajority/role_keep_majority_spec.conf") {
  nodeConfig(node1, node2, node3)(ConfigFactory.parseString("""akka.cluster.roles = ["foo"]"""))
}
