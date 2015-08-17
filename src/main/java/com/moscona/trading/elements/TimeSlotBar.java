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

package com.moscona.trading.elements;

import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.trading.ITickStreamRecord;

/**
 * Created: Apr 14, 2010 11:26:48 AM
 * By: Arnon Moscona
 * This is the core class that makes up the basic unit of stock charts.
 * It is a bar that starts its life as an accumulator of ticks for the same symbol and can be closed,
 * producing a normal bar.
 */
public class TimeSlotBar extends Bar implements ICumulativeBar {
    private boolean isClosed;
    private int tickCount;
    private boolean markedMissingData;

    // todo need to track more detail in the bar in order to do outlier detection. IT-78

    public TimeSlotBar() throws InvalidArgumentException {
        super(0,0,0,0,0);
        isClosed = false;
        tickCount = 0;
        markedMissingData = false;
    }

    /**
     * resets the values so that the bar can be reused
     */
    @Override
    public void reset() {
        setOpenCents(0);
        setCloseCents(0);
        setLowCents(0);
        setHighCents(0);
        setVolume(0);

        isClosed = false;
    }

    /**
     * Adds a record to an open bar
     * @param record
     */
    @Override
    public void add(ITickStreamRecord record) throws InvalidArgumentException {
        if (isClosed) {
            // todo deal with attempting to add a record to a bar that's already closed
            return;
        }

        float price = record.getPrice();

        if (!anyValueNonZero()) {
            // all values zero - this has never been used before
            setOpen(price);
            setLow(price);
            setHigh(price);
        }

        setClose(price);
        setHigh(Math.max(price, getHigh()));
        setLow(Math.min(price,getLow()));
        setVolume(getVolume() + record.getQuantity());

        validate();
        tickCount++;
    }

    @Override
    public boolean hasData() {
        return anyValueNonZero();
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void close() {
        isClosed = true;
    }

    @Override
    public int getTickCount() {
        return tickCount;
    }

    @Override
    public String toString() {
        return super.toString()+" ticks:"+tickCount+(isClosed?" [closed]":" [open]");
    }

    /**
     * marks the bar as missing data. What this means is that a gap was notes where this should probably have had
     * data, but none was observed. This does not necessarily cover all cases of missing data, but when marked in such
     * a way we're pretty confident that we lost data. When we are confident that we lost data, we are not necessarily
     * confident that we lost data in this particular bar, but that data was certainly lost in this period.
     *
     * Marking partial does not mean that *all* data is missing either. It could be that we still got some data for the
     * bar or for the period, but we certainly missed data within the period, and have no way to tell whether this bar
     * was affected or not.
     */
    @Override
    public void markMissingData() {
        markedMissingData = true;
    }

    /**
     * @see #markMissingData()
     * @return true if the bar was marked as possibly missing data.
     */
    @Override
    public boolean isMarkedMissingData() {
        return markedMissingData;
    }
}
