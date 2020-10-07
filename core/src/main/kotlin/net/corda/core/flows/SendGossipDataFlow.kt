package net.corda.core.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.identity.Party
import net.corda.core.node.services.DataServiceInterface
import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.unwrap
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import java.io.Serializable
import javax.transaction.Transactional

@InitiatingFlow
@StartableByRPC
@StartableByService
class SendGossipDataFlow(
        private val participants: List<Party>,
        private val isInitiatorNode: Boolean? = true
) : FlowLogic<Unit>(){

    @Suspendable
    @Transactional
    override fun call() {

        val mutableParticipants = participants.toMutableList()
        val message = Message(1, "Hello")

        //if it is the initiator node we need to persist the message into its db
        //later on it is going to be false to avoid duplicated entries in the db
        if(isInitiatorNode == true) {
            serviceHub.withEntityManager {
                this.persist(message)
                flush()
            }
        }

        //selects a random number of nodes randomly from the list, starts gossiping
        //and removes the element from the list to minimise unnecessary message sends
        if(mutableParticipants.size != 0 && mutableParticipants.size != 1) {
            val randomRange = (1..mutableParticipants.size).shuffled().first()
            val chosenNodes = mutableListOf<Party>()

            for(x in 0 until randomRange) {
                val randomNum = (0 until mutableParticipants.size).shuffled().first()
                val randomNode = mutableParticipants.removeAt(randomNum)
                chosenNodes.add(randomNode)
            }

            val messageData = MessageWithRecipients(message, mutableParticipants)

            chosenNodes.forEach {
                val session = initiateFlow(it)
                session.send(messageData)
            }
        } else if(mutableParticipants.size == 1) {
            //if the participants size decreases to 1, we pass an empty list of participants to break the flow execution
            val messageData = MessageWithRecipients(message, mutableListOf())
            val session = initiateFlow(mutableParticipants.first())
            session.send(messageData)
        }
    }
}

@InitiatedBy(SendGossipDataFlow::class)
class SendGossipDataResponderFlow(
        private val session: FlowSession
) : FlowLogic<Unit>(){

    @Suspendable
    @Transactional
    override fun call() {
        val receivedMessage = session.receive<MessageWithRecipients>().unwrap { it }
        val message = receivedMessage.message

        serviceHub.withEntityManager {
            val versionIds = createQuery("select m.id from Message m order by m.id desc").resultList.map { it as Int }

            if(versionIds.isEmpty() || versionIds.first() < message.versionId){
                this.persist(message)
                logger.info(message.message)
            }

            flush()
        }

        //we won't trigger the subflow if we don't have any participants left
        if(receivedMessage.participants.isNotEmpty()) {
            DataServiceInterface.gossip(receivedMessage.participants, false)
            //subFlow(SendGossipDataFlow(receivedMessage.participants, false))
        }
    }
}

object MessageSchema
object MessageSchemaV1 : MappedSchema(schemaFamily = MessageSchema::class.java, version = 1, mappedTypes = listOf(Message::class.java)) {
    override val migrationResource: String? get() = "message-changelog-master"
}

@CordaSerializable
@Entity(name = "Message")
@Table(name = "message")
data class Message(
        @Id
        @Column(name = "id", nullable = false)
        val versionId: Int,

        @Column(name = "message", nullable = false)
        val message: String
) : Serializable

@CordaSerializable
data class MessageWithRecipients(
        val message: Message,
        val participants: List<Party>
)