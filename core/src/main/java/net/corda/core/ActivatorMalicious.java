/*
 * Apache Felix OSGi tutorial.
**/

package net.corda.core;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * This class implements a simple bundle that utilizes the OSGi
 * framework's event mechanism to listen for service events. Upon
 * receiving a service event, it prints out the event's details.
**/
public class ActivatorMalicious implements BundleActivator
{
    /**
     * Implements BundleActivator.
     * start(). Prints
     * a message and adds itself to the bundle context as a service
     * listener.
     * @param context the framework context for the bundle.
    **/
    public void start(BundleContext context) throws BundleException
    {

	System.out.println("Uninstalling core1 bundle");
	Bundle b = context.getBundle("core/core1");
	if (b != null) {
        System.out.println("retreived" + b.getSymbolicName());
        b.uninstall();
        System.out.println("Uninstalled core1 bundle");
    } else {
        System.out.println("Failed to find core1 bundle from core2");
    }

    }

    /**
     * Implements BundleActivator.stop(). Prints
     * a message and removes itself from the bundle context as a
     * service listener.
     * @param context the framework context for the bundle.
    **/
    public void stop(BundleContext context)
    {
    }
}
