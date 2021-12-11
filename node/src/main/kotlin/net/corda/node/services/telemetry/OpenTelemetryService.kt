package net.corda.node.services.telemetry

import net.corda.core.node.services.TelemetryService
import net.corda.core.serialization.SingletonSerializeAsToken

class OpenTelemetryService() : SingletonSerializeAsToken(), TelemetryService {

}