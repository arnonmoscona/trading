package com.moscona.trading;

import com.moscona.exceptions.InvalidStateException;
import com.moscona.trading.ServicesBundle;

import java.util.List;

/**
 * Created: Apr 30, 2010 2:19:26 PM
 * By: Arnon Moscona
 */
public interface IServicesBundleManager {
    /**
     * A factory method to create services bundles compatible with the configuration. Used to create the default
     * services bundle but also for any code that needs to create private instances, predominantly required for
     * anything that runs in a separate thread.
     * @return a new instance of a a services bundle
     * @throws com.moscona.exceptions.InvalidStateException if cannot construct the appropriate classes
     */
    ServicesBundle createServicesBundle() throws InvalidStateException;

    /**
     * returns all services bundles ever created by this manager
     * the number should be small - roughly the number of threads in the server)
     * @return a list of all the budles created by this server
     */
    List<ServicesBundle> getServicesBundles();
}
