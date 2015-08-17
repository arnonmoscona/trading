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
import com.moscona.trading.ITickStreamRecord;
import com.moscona.trading.excptions.MissingSymbolException;
import com.moscona.util.TimeHelper;

import java.util.AbstractMap;

/**
 * Created: Jul 7, 2010 12:44:38 PM
 * By: Arnon Moscona
 * A class that is used as a stand-in for a tick in non-real time contexts that require an ITickStreamRecord.
 * It basically does not use the byte arrays and does not use the mappings.
 * It just stores the data and holds it.
 * It is used mainly when working with ticks from a historic data source, which are then fed to some other process
 * (not into the server core)
 */
public class HeavyTickStreamRecord implements ITickStreamRecord {
    private String symbol=null;
    private float price=0.0f;
    private int quantity=0;
    private int timestamp=0;
    private int insertionTimestamp=-1;
    private boolean initialized = false;
    public static final double HALF_A_PENNY = 0.005;

    /**
     * @param transactionTimestampOffset - the transaction time offset in milliseconds from the Epoch in US/Eastern time zone.
     * @param symbol                     - the symbol to use. Must already exist in the symbol table
     * @param price                      - the price of the transaction
     * @param quantity                   - the number of shares traded
     * @param forwardMap                 - IGNORED
     * @param backwardMap                - IGNORED
     * @return this
     * @throws com.moscona.exceptions.InvalidArgumentException
     *          if any of the conversions don't work
     */
    @Override
    public ITickStreamRecord init(long transactionTimestampOffset, String symbol, float price, int quantity, AbstractMap<String, Integer> forwardMap, AbstractMap<Integer, String> backwardMap) throws InvalidArgumentException, MissingSymbolException {
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = TimeHelper.timeStampRelativeToMidnight(transactionTimestampOffset);
        initialized = true;
        return this;
    }

    /**
     * Returns a zero length byte array. DO NOT USE THIS IMPLEMENTATION.
     *
     * @return the backing array of the instance
     */
    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    /**
     * NON FUNCTIONAL - DO NOT USE
     * @param newValue
     * @return
     * @throws InvalidArgumentException
     */
    @Override
    public ITickStreamRecord replaceBytes(byte[] newValue) throws InvalidArgumentException {
        throw new InvalidArgumentException("This class does not implement this method");
    }

    @Override
    public int getTransactionTimestamp() {
        return timestamp;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public float getPrice() throws InvalidArgumentException {
        return price;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }


    @Override
    public int getInsertionTimestamp() {
        return insertionTimestamp;
    }

    /**
     * Sets the insertion timestamp to the internal representation of right now
     */
    @Override
    public void setInsertionTimestamp() {
        insertionTimestamp = TimeHelper.now();
    }

    /**
     * Compares the value to the other record, ignoring the insertion timestamp
     *
     * @param other - the record to compare to
     * @return true if all values are the same
     */
    @Override
    public boolean equalsWithoutInsertionTs(ITickStreamRecord other) {
        if (!initialized) {
            return false;
        }
        try {
            return Math.abs(other.getPrice()-price)< HALF_A_PENNY && other.getQuantity()==quantity && other.getTransactionTimestamp() == timestamp && other.getSymbol().equals(symbol);
        }
        catch (InvalidArgumentException e) {
            return false;
        }
    }
}
