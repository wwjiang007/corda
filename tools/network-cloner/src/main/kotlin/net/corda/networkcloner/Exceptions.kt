package net.corda.networkcloner

import java.lang.RuntimeException

class NoDestinationTransactionFoundException(sourceTransactionId : String) : RuntimeException("No destination transaction found for source transaction id $sourceTransactionId")

class FailedAssumptionException(message : String) : RuntimeException(message)