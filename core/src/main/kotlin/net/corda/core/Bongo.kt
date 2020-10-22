package net.corda.core;

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.osgi.service.log.Logger
import org.osgi.service.log.LoggerFactory

@Component(immediate = true)
class Bongo @Activate constructor(
        @Reference(service = LoggerFactory::class)
        private val logger: Logger
) {
    init {
        System.err.println("-------------- Initialising Bongo!")
        logger.error("Initialising Bongo {}", this::class.java)
    }

    @Activate
    fun doIt() {
        //some guava code to trigger osgi imports
        val items: Map<*, *> = ImmutableMap.of("coin", 3, "glass", 4, "pencil", 1)
        val fruits: List<String> = Lists.newArrayList("orange", "banana", "kiwi",
                "mandarin", "date", "quince")

        System.err.println("-------------- Activated Bongo!")
        logger.error("Activated Bongo {}", this::class.java)
    }
}


