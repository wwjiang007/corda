package net.corda.core;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

public class LogWriter implements LogListener
{
    // Invoked by the log service implementation for each log entry
    public void logged(LogEntry entry)
    {
        System.out.println(entry.getMessage());
    }
}