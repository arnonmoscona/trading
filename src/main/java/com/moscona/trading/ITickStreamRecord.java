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
import com.moscona.trading.excptions.MissingSymbolException;

import java.util.AbstractMap;

/**
 * Created by Arnon on 5/5/2014.
 */
public interface ITickStreamRecord {
    /**
     * @param transactionTimestampOffset - the transaction time offset in milliseconds from the Epoch in US/Eastern time zone.
     * @param symbol - the symbol to use. Must already exist in the symbol table
     * @param price - the price of the transaction
     * @param quantity - the number of shares traded
     * @param forwardMap - the map to translate symbols to integers
     * @param backwardMap - the map to translate integers to symbols
     * @throws com.moscona.exceptions.InvalidArgumentException if any of the conversions don't work
     * @return this
     */
    @SuppressWarnings({"MethodWithTooManyParameters"})
    public ITickStreamRecord init(long transactionTimestampOffset, String symbol, float price, int quantity,
                            AbstractMap<String,Integer> forwardMap,
                            AbstractMap<Integer,String> backwardMap)
            throws InvalidArgumentException, MissingSymbolException;
    /**
     * Returns the raw backing array. Caution! This is not a clone of the array but the actual array.
     * This operation is not only dangerous, but also very not thread safe. It is, however very efficient and creates
     * no extra memory allocation or temporary objects.
     * @return the backing array of the instance
     */
    byte[] toBytes();

    public ITickStreamRecord replaceBytes(byte[] newValue) throws InvalidArgumentException;

    int getTransactionTimestamp();

    String getSymbol();

    float getPrice() throws InvalidArgumentException;

    int getQuantity();

    int getInsertionTimestamp();

    /**
     * Sets the insertion timestamp to the internal representation of right now
     */
    void setInsertionTimestamp();

    /**
     * Compares the value to the other record, ignoring the insertion timestamp
     * @param other - the record to compare to
     * @return true if all values are the same
     */
    public boolean  equalsWithoutInsertionTs(ITickStreamRecord other);

    public String toString();
}
