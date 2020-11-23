package net.corda.v5.notary

import com.codahale.metrics.MetricRegistry
import net.corda.core.cordapp.CordappContext
import net.corda.core.identity.PartyAndCertificate
import java.time.Clock

/**
 * Provides properties which are required by notary services.
 *
 * This is a public interface which maintains binary compatibility within a major release.
 */
interface NotaryServiceProperties {
    /** Provides the CorDapp context. Used to gain access to notary CorDapp configuration. */
    val cordappContext: () -> CordappContext
    /** Indicates whether to instantiate a validating or non-validating notary service. */
    val isValidating: Boolean
    /** The identity of the logical notary service, shared by all notary workers in the cluster. */
    val notaryServiceIdentity: PartyAndCertificate
    /** The identity of this specific notary worker */
    val notaryWorkerIdentity: PartyAndCertificate
    /** Batch signing function provided by the node, used for signing notarisation requests. */
    val batchSigningFunction: BatchSigningFunction
    /** Provides access to the Corda nodes clock. */
    val nodeClock: Clock
    /** Provides access to the Corda nodes metric registry. */
    val metricRegistry: MetricRegistry
}