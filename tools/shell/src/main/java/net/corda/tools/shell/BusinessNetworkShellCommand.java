package net.corda.tools.shell;

import net.corda.nodeapi.internal.businessnetwork.BusinessNetworkOperationsRPCOps;
import org.crsh.cli.Command;
import org.crsh.cli.Named;
import org.jetbrains.annotations.NotNull;

import static net.corda.tools.shell.InteractiveShell.runCreateBusinessNetwork;
import static net.corda.tools.shell.InteractiveShell.runCreateGroup;

@Named("bn")
public class BusinessNetworkShellCommand extends InteractiveShellCommand<BusinessNetworkOperationsRPCOps> {

    @NotNull
    @Override
    public Class<BusinessNetworkOperationsRPCOps> getRpcOpsClass() {
        return BusinessNetworkOperationsRPCOps.class;
    }

    @Command
    public void createBusinessNetwork() {
        runCreateBusinessNetwork(ops());
    }

    @Command
    public void createGroup() {
        runCreateGroup(ops());
    }
}
