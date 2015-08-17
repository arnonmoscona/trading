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
import com.moscona.util.IAlertService;
import com.moscona.util.monitoring.stats.IStatsService;
import com.moscona.util.app.lifecycle.EventBase;
import net.engio.mbassy.bus.IMBassador;

/**
 * Created: Apr 27, 2010 2:35:02 PM
 * By: Arnon Moscona
 */
public class ServicesBundle implements IServicesBundle {
    IStatsService statsService=null;
    IAlertService alertService=null;
    IMBassador<EventBase> eventPublisher=null;

    public ServicesBundle() {

    }

    public ServicesBundle(IStatsService statsService, IAlertService alertService) {
        this.statsService = statsService;
        this.alertService = alertService;
        this.eventPublisher = null;
    }

    public ServicesBundle(IStatsService statsService, IAlertService alertService, IMBassador<EventBase> eventPublisher) {
        this.statsService = statsService;
        this.alertService = alertService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public IStatsService getStatsService() {
        return statsService;
    }

    public void setStatsService(IStatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public IAlertService getAlertService() {
        return alertService;
    }

    @Override
    public IMBassador<EventBase> getEventPublisher() {
        return eventPublisher;
    }

    public void setEventPublisher(IMBassador<EventBase> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * A helper method for contexts that need to make sure that their services bundle is different in every aspect from another services bundle.
     * @param other  the other services bundle
     * @param whoIsTheFirst  a string identifying which instance the first parameter is
     * @param whoIsTheOther  a string identifying which instance the second parameter is
     * @throws InvalidArgumentException if the two bundles are the same one or any of the services in them match.
     */
    @Override
    public void requireAllDifferentFrom(IServicesBundle other, String whoIsTheFirst, String whoIsTheOther) throws InvalidArgumentException {
        if (other==null) {
            throw new InvalidArgumentException("ServicesBundle.requireAllDifferentFrom() - the first parameter was null!!");
        }
        String msgPart1 = whoIsTheFirst+" may not use the same ";
        String msgPart2 = " as "+whoIsTheOther+" - reconfigure it to use a private one for thread safety.";
        if (other == this) {
            throw new InvalidArgumentException(msgPart1+"services bundle"+msgPart2);
        }
        if (other.getStatsService() == statsService) {
            throw new InvalidArgumentException(msgPart1+"stats service"+msgPart2);
        }
        if (other.getAlertService() == alertService) {
            throw new InvalidArgumentException(msgPart1+"alert service"+msgPart2);
        }
    }

    public void setAlertService(IAlertService alertService) {
        this.alertService = alertService;
    }
}
