package net.corda.tools.shell;

import net.corda.core.flows.bn.Membership;
import net.corda.nodeapi.internal.businessnetwork.BusinessNetworkOperationsRPCOps;
import org.crsh.cli.Argument;
import org.crsh.cli.Command;
import org.crsh.cli.Named;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static net.corda.tools.shell.InteractiveShell.runActivateMembership;
import static net.corda.tools.shell.InteractiveShell.runCreateBusinessNetwork;
import static net.corda.tools.shell.InteractiveShell.runGetMembershipList;
import static net.corda.tools.shell.InteractiveShell.runOnboardMembership;
import static net.corda.tools.shell.InteractiveShell.runRevokeMembership;
import static net.corda.tools.shell.InteractiveShell.runSuspendMembership;

@Named("bn")
public class BusinessNetworkShellCommand extends InteractiveShellCommand<BusinessNetworkOperationsRPCOps> {

    @NotNull
    @Override
    public Class<BusinessNetworkOperationsRPCOps> getRpcOpsClass() {
        return BusinessNetworkOperationsRPCOps.class;
    }

    @Command
    public Membership createBusinessNetwork() {
        return runCreateBusinessNetwork(ops());
    }

    @Command
    public Membership onboardMembership(@Argument String networkId, @Argument String party) {
        Objects.requireNonNull(networkId);
        Objects.requireNonNull(party);
        return runOnboardMembership(ops(), networkId, party);
    }

    @Command
    public Membership activateMembership(@Argument String id) {
        Objects.requireNonNull(id);
        return runActivateMembership(ops(), Long.parseLong(id));
    }

    @Command
    public Membership suspendMembership(@Argument String id) {
        Objects.requireNonNull(id);
        return runSuspendMembership(ops(), Long.parseLong(id));
    }

    @Command
    public void revokeMembership(@Argument String id) {
        Objects.requireNonNull(id);
        runRevokeMembership(ops(), Long.parseLong(id));
    }

    @Command
    public List<Membership> getMembershipList(@Argument String networkId) {
        Objects.requireNonNull(networkId);
        return runGetMembershipList(ops(), networkId);
    }
}
