package com.moscona.trading.elements;

import com.moscona.exceptions.InvalidArgumentException;


/**
 * Created by Arnon on 5/5/2014.
 * Note: refactored from another project. Perhaps not the cleanest design. The original project was very monitoring focused
 * and used arrays whenever possible. This focus may reemerge in this context as well, and so both for ease of code
 * refactoring as well as for keeping it consistent with the original (mostly), I am not redesigning this class.
 */
public class SymbolChart<T extends IBar> {
    private String symbol;
    private int startTimeStamp; // server internal timestamp
    private int endTimeStamp;   // server internal timestamp
    private int granularityMillis;
    private IBar[] bars;
    private int lastAvailableBar;
    private int nonNullBars;

    public SymbolChart(String symbol, int startTimeStamp, int endTimeStamp, int granularityMillis) throws InvalidArgumentException {
        this.symbol = symbol;
        this.startTimeStamp = startTimeStamp;
        this.endTimeStamp = endTimeStamp;
        this.granularityMillis = granularityMillis;

        if (endTimeStamp < startTimeStamp + granularityMillis) {
            throw new InvalidArgumentException("The space between the end time stamp and the start timestamp does not even allow for one bar given the granularity");
        }

        int size = (endTimeStamp - startTimeStamp) / granularityMillis;
        bars = new IBar[size];
        lastAvailableBar = -1;
        nonNullBars = 0;
    }

    /**
     * Is there any data in the chart?
     * @return true if there is any data here
     */
    public boolean hasData() {
        return lastAvailableBar >= 0 && nonNullBars > 0;
    }

    /**
     * Adds another bar to the chart. Puts a reference to it (not a copy!) in the next available slot.
     * @param bar the bar to add
     */
    public void addBar(T bar) {
        setBar(bar, lastAvailableBar+1);
    }

    public void setBar(T bar, int slot) {
        bars[slot] = bar;
        if (lastAvailableBar < slot) {
            lastAvailableBar = slot;
        }
        if (bar != null) {
            nonNullBars++;
        }
    }

    public int size() {
        return lastAvailableBar + 1;
    }

    public int capacity() {
        return bars.length;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getStartTimeStamp() {
        return startTimeStamp;
    }

    public int getEndTimeStamp() {
        return endTimeStamp;
    }

    public int getGranularityMillis() {
        return granularityMillis;
    }

    @SuppressWarnings("unchecked") // cast of array type to T
    public T getBar(int i) {
        if (i<0 || i>lastAvailableBar) {
            return null;
        }
        return (T) bars[i];
    }

    public int getLastAvailableBarIndex() {
        return lastAvailableBar;
    }

    public int getNonNullBarsCount() {
        return nonNullBars;
    }

    // todo some kind of iterator for a chart (only non nulls?)
}

