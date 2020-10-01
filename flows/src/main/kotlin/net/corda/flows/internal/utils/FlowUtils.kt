package net.corda.flows.internal.utils

import net.corda.core.flows.FlowLogic
import net.corda.flows.internal.IdempotentFlow

/** Checks if this flow is an idempotent flow. */
fun Class<out FlowLogic<*>>.isIdempotentFlow(): Boolean {
    return IdempotentFlow::class.java.isAssignableFrom(this)
}

