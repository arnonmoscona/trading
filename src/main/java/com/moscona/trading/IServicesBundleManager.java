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
