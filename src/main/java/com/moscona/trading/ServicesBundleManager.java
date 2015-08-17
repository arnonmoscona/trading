/*
 * Copyright (c) 2015. Arnon Moscona
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.moscona.trading;

import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.exceptions.InvalidStateException;
import com.moscona.util.IAlertService;
import com.moscona.util.monitoring.stats.IStatsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created: Apr 29, 2010 5:37:52 PM
 * By: Arnon Moscona
 * A class that acts both as a factory for services bundles and a tracker of what services bundles were created
 */
public class ServicesBundleManager<Config extends IServiceBundleManagerConfig> implements IServicesBundleManager {
    private String statsServiceClassName;
    private String alertServiceClassName;
    private Class statsServiceClass;
    private Class alertServiceClass;

    private Config config;

    ArrayList<ServicesBundle> servicesBundles; // collects all bundles created by this manager

    public ServicesBundleManager(Config config) throws InvalidArgumentException {
        this.config = config;
        this.servicesBundles = new ArrayList<ServicesBundle>();

        this.statsServiceClassName = config.getStatsServiceClassName();
        this.alertServiceClassName = config.getAlertServiceClassName();
        try {
            statsServiceClass = Class.forName(statsServiceClassName);
            alertServiceClass = Class.forName(alertServiceClassName);
        }
        catch(ClassNotFoundException ex) {
            throw new InvalidArgumentException("Class not found"+ex,ex);
        }
        validate();
    }

    private void validate() throws InvalidArgumentException {
        if (! IStatsService.class.isAssignableFrom(statsServiceClass)) {
            throw new InvalidArgumentException("stats service class must be a IStatsService");
        }
        if (! IAlertService.class.isAssignableFrom(alertServiceClass)) {
            throw new InvalidArgumentException("alert service class must be a IAlertService");
        }
    }

    /**
     * A factory method to create services bundles compatible with the configuration. Used to create the default
     * services bundle but also for any code that needs to create private instances, predominantly required for
     * anything that runs in a separate thread.
     * @return a new instance of a a services bundle
     * @throws com.moscona.exceptions.InvalidStateException if cannot construct the appropriate classes
     */
    @Override
    public ServicesBundle createServicesBundle() throws InvalidStateException {
        try {
            IStatsService statsService = (IStatsService) statsServiceClass.newInstance();
            IAlertService alertService = (IAlertService) alertServiceClass.newInstance();
            initFromThisConfig(statsService);
            initFromThisConfig(alertService);

            ServicesBundle bundle = new ServicesBundle(statsService, alertService);
            servicesBundles.add(bundle);
            return bundle;

        } catch (Exception e) {
            throw new InvalidStateException("Error while constructing a services bundle "+e,e);
        }
    }

    // FIXME This is a mess. Must be replaced by some clean dependency injection
    private void initFromThisConfig(Object plugin) throws InvalidStateException, InvalidArgumentException {
        try {
            if (plugin == null) {
                throw new InvalidArgumentException("plugin may not be null");
            }
            if (isIServerConfigInitializable(plugin)) {
                ((IConfigInitializable<Config>) plugin).init(config);
            }
        } catch (Exception e) {
            throw new InvalidArgumentException("Error while trying to process " + plugin + " : " + e, e);
        }
    }

    private boolean isIServerConfigInitializable(Object obj) {
        return IConfigInitializable.class.isAssignableFrom(obj.getClass());
    }

    /**
     * returns all services bundles ever created by this manager
     * the number should be small - roughly the number of threads in the server)
     * @return a list of all the budles created by this server
     */
    @Override
    public List<ServicesBundle> getServicesBundles() {
        return Collections.unmodifiableList(servicesBundles);
    }
}
