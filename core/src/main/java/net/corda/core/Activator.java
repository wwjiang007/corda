package net.corda.core;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;

/**
 * This class implements a simple bundle that utilizes the OSGi
 * framework's event mechanism to listen for service events. Upon
 * receiving a service event, it prints out the event's details.
 **/
public class Activator implements BundleActivator
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
        System.out.println("Activating log reader service");
        ServiceReference ref = context.getServiceReference(LogReaderService.class.getName());
        if (ref != null)
        {
            LogReaderService reader = (LogReaderService) context.getService(ref);
            reader.addLogListener(new LogWriter());
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
