package com.moscona.trading;

import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.util.IAlertService;
import com.moscona.util.IStatsService;
import com.moscona.util.app.lifecycle.EventBase;
import net.engio.mbassy.bus.IMBassador;

/**
 * Created: Apr 27, 2010 2:35:42 PM
 * By: Arnon Moscona
 * A bundle of (mutable) services that can be attached to a ServicesClient
 */
public interface IServicesBundle {
    public IStatsService getStatsService();
    public IAlertService getAlertService();

    IMBassador<EventBase> getEventPublisher();

    /**
     * A helper method for contexts that need to make sure that their services bundle is different in every aspect from another services bundle.
     * @param other  the other services bundle
     * @param whoIsTheFirst  a string identifying which instance the first parameter is
     * @param whoIsTheOther  a string identifying which instance the second parameter is
     * @throws InvalidArgumentException if the two bundles are the same one or any of the services in them match.
     */
    public void requireAllDifferentFrom(IServicesBundle other, String whoIsTheFirst, String whoIsTheOther) throws InvalidArgumentException;
}
