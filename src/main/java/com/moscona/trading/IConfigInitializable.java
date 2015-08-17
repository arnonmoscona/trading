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

/**
 * Created: Apr 29, 2010 9:02:50 AM
 * By: Arnon Moscona
 * An interface for classes that have:
 * a default constructor
 * and a method init(ServerConfig config)
 * where calling the two in sequence produces a valid instance
 */
public interface IConfigInitializable<Config> {
    /**
     * A method which when called on an instance produced by a default constructor, would either make this instance
     * fully functional, or would result in an exception indicating that the instance should be thrown away.
     * @param config an instance of the server configuration to work with.
     * @throws InvalidArgumentException if any of the relevant configuration entries is wrong
     * @throws com.moscona.exceptions.InvalidStateException if after trying everything else, was still in an invalid state.
     */
    public void init(Config config) throws InvalidArgumentException, InvalidStateException;
}