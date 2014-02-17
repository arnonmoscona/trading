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