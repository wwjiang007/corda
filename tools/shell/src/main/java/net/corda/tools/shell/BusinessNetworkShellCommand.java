package net.corda.tools.shell;

import net.corda.core.messaging.BusinessNetworkOperatorRPCOps;
import net.corda.tools.shell.InteractiveShellCommand;
import org.crsh.cli.Command;
import org.crsh.cli.Named;
import org.jetbrains.annotations.NotNull;

import static net.corda.tools.shell.InteractiveShell.runCreateBusinessNetwork;

@Named("bn")
public class BusinessNetworkShellCommand extends InteractiveShellCommand<BusinessNetworkOperatorRPCOps> {

    @NotNull
    @Override
    public Class<BusinessNetworkOperatorRPCOps> getRpcOpsClass() {
        return BusinessNetworkOperatorRPCOps.class;
    }

    @Command
    public void createBusinessNetwork() {
        runCreateBusinessNetwork(ops());
    }
}
