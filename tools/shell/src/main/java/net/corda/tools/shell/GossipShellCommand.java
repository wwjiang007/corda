package net.corda.tools.shell;

import net.corda.nodeapi.internal.businessnetwork.GossipRPCOps;
import org.crsh.cli.Argument;
import org.crsh.cli.Command;
import org.crsh.cli.Man;
import org.crsh.cli.Named;
import org.crsh.cli.Usage;
import org.jetbrains.annotations.NotNull;
import java.util.List;

@Named("gossip")
public class GossipShellCommand extends InteractiveShellCommand<GossipRPCOps> {

    @NotNull
    @Override
    public Class<GossipRPCOps> getRpcOpsClass() {
        return GossipRPCOps.class;
    }

    @Man("Sends data to the given participants")
    @Usage("gossip send participants: [\"O=BigCorporation,L=New York,C=US\",\"O=BankOfCorda,L=London,C=GB\"]")
    @Command
    public void sendGossip(@Usage("The targets of the gossip, should include everyone who should receive the information") @Argument String participants) {
        ops().gossiping(participants);
    }
}
