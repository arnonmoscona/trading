package com.moscona.trading.elements;

import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.trading.ITickStreamRecord;
import com.moscona.trading.elements.IBar;

/**
 * Created by Arnon on 5/5/2014.
 */
public interface ICumulativeBar extends IBar {
    /**
     * resets the values so that the bar can be reused
     */
    void reset();

    /**
     * Adds a record to an open bar
     * @param record
     */
    void add(ITickStreamRecord record) throws InvalidArgumentException;

    boolean hasData();

    boolean isClosed();

    void close();

    int getTickCount();

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
    void markMissingData();

    /**
     * @see #markMissingData()
     * @return true if the bar was marked as possibly missing data.
     */
    boolean isMarkedMissingData();

//    static ICumulativeBar[] createArray(int size) {
//        return new ICumulativeBar[size];
//    }
}

