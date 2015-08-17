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

package com.moscona.trading.streaming;

import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.trading.elements.TimeSlotBar;
import com.moscona.util.TimeHelper;

import java.util.Calendar;

/**
 * Created: Jul 9, 2010 2:08:28 PM
 * By: Arnon Moscona
 */
public class TimeSlotBarWithTimeStamp extends TimeSlotBar {
    private Calendar timestamp = null;

    public TimeSlotBarWithTimeStamp() throws InvalidArgumentException {
        super();
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return super.toString()+" timestamp: "+ TimeHelper.toString(timestamp);
    }
}
