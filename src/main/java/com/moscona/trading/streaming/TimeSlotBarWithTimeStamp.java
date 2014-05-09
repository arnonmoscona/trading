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
