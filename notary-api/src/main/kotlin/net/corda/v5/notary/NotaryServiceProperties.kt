package net.corda.v5.notary

import com.codahale.metrics.MetricRegistry
import net.corda.core.cordapp.CordappContext
import net.corda.core.identity.PartyAndCertificate
import java.time.Clock

interface NotaryServiceProperties {
    val cordappContext: () -> CordappContext
    val notaryServiceIdentity: PartyAndCertificate
    val notaryWorkerIdentity: PartyAndCertificate
    val defaultBatchSigningFunction: BatchSigningFunction
    val nodeClock: Clock
    val metricRegistry: MetricRegistry
}