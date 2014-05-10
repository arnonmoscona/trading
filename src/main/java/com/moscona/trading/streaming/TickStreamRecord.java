package com.moscona.trading.streaming;

import com.moscona.exceptions.InvalidArgumentException;
import com.moscona.trading.ITickStreamRecord;
import com.moscona.trading.excptions.MissingSymbolException;
import com.moscona.util.TimeHelper;
import com.moscona.util.transformation.ByteArrayHelper;

import java.nio.ByteBuffer;
import java.util.AbstractMap;

/**
 * Created: Mar 18, 2010 2:00:24 PM
 * By: Arnon Moscona
 * The basic record that goes into the server buffer.
 */
@SuppressWarnings({"ConstructorWithTooManyParameters"})
public class TickStreamRecord implements ITickStreamRecord {
    public static final int TRANSACTION_TS_FIELD_LENGTH = 4;
    public static final int SYMBOL_FIELD_LENGTH = 2;
    public static final int PRICE_FIELD_LENGTH = 4;
    public static final int QUANTITY_FIELD_LENGTH = 4; // could be 3 but can have a more efficient implementation with 4
    public static final int INSERTION_TS_FIELD_LENGTH = 4;
    public static final int RECORD_LENGTH = TRANSACTION_TS_FIELD_LENGTH + SYMBOL_FIELD_LENGTH + PRICE_FIELD_LENGTH +
            QUANTITY_FIELD_LENGTH + INSERTION_TS_FIELD_LENGTH;

    public static final int TRANSACTION_TS_FIELD_OFFSET = 0;
    @SuppressWarnings({"PointlessArithmeticExpression"})
    public static final int SYMBOL_FIELD_OFFSET = TRANSACTION_TS_FIELD_OFFSET + TRANSACTION_TS_FIELD_LENGTH;
    public static final int PRICE_FIELD_OFFSET = SYMBOL_FIELD_OFFSET + SYMBOL_FIELD_LENGTH;
    public static final int QUANTITY_FIELD_OFFSET = PRICE_FIELD_OFFSET + PRICE_FIELD_LENGTH;
    public static final int INSERTION_TS_FIELD_OFFSET = QUANTITY_FIELD_OFFSET + QUANTITY_FIELD_LENGTH;

    private ByteBuffer record;
    private AbstractMap<String,Integer> symbolToCode;
    private AbstractMap<Integer,String> codeToSymbol;
    private static final double HALF_TICK = 0.05;


    public TickStreamRecord() {

    }

    public TickStreamRecord(AbstractMap<String,Integer> forwardMap,
                            AbstractMap<Integer,String> backwardMap) throws InvalidArgumentException {
        setSymbolToCode(forwardMap);
        setCodeToSymbol(backwardMap);
    }

    /**
     * @param transactionTimestampOffset - the transaction time offset in milliseconds from the Epoch in US/Eastern time zone.
     * @param symbol - the symbol to use. Must already exist in the symbol table
     * @param price - the price of the transaction
     * @param quantity - the number of shares traded
     * @param forwardMap - the map to translate symbols to integers
     * @param backwardMap - the map to translate integers to symbols
     * @throws com.moscona.exceptions.InvalidArgumentException if any of the conversions don't work
     */
    public TickStreamRecord(long transactionTimestampOffset, String symbol, float price, int quantity,
                            AbstractMap<String,Integer> forwardMap,
                            AbstractMap<Integer,String> backwardMap)
            throws InvalidArgumentException, MissingSymbolException {
        init(transactionTimestampOffset, symbol, price, quantity, forwardMap, backwardMap);
    }

    /**
     * @param internalTimeStamp - the transaction time expressed as an internal server timestamp
     * @param symbol - the symbol to use. Must already exist in the symbol table
     * @param price - the price of the transaction
     * @param quantity - the number of shares traded
     * @param forwardMap - the map to translate symbols to integers
     * @param backwardMap - the map to translate integers to symbols
     * @throws com.moscona.exceptions.InvalidArgumentException if any of the conversions don't work
     */
    public TickStreamRecord(int internalTimeStamp, String symbol, float price, int quantity,
                            AbstractMap<String,Integer> forwardMap,
                            AbstractMap<Integer,String> backwardMap)
            throws InvalidArgumentException, MissingSymbolException {
        long transactionTimestampOffset= TimeHelper.lastMidnightInMillis()+internalTimeStamp;
        init(transactionTimestampOffset, symbol, price, quantity, forwardMap, backwardMap);
    }

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
    @Override
    @SuppressWarnings({"MethodWithTooManyParameters", "AssignmentToCollectionOrArrayFieldFromParameter"})
    public final ITickStreamRecord init(long transactionTimestampOffset, String symbol, float price, int quantity,
                            AbstractMap<String,Integer> forwardMap,
                            AbstractMap<Integer,String> backwardMap)
            throws InvalidArgumentException, MissingSymbolException {

        int internal_ts = TimeHelper.convertToInternalTs(transactionTimestampOffset);
        return init(internal_ts, symbol, price, quantity, forwardMap, backwardMap);
    }

    /**
     * @param internalTimestamp - the transaction time as an internal server timestamp
     * @param symbol - the symbol to use. Must already exist in the symbol table
     * @param price - the price of the transaction
     * @param quantity - the number of shares traded
     * @param forwardMap - the map to translate symbols to integers
     * @param backwardMap - the map to translate integers to symbols
     * @throws com.moscona.exceptions.InvalidArgumentException if any of the conversions don't work
     * @return this
     */
    @SuppressWarnings({"MethodWithTooManyParameters", "AssignmentToCollectionOrArrayFieldFromParameter"})
    public final ITickStreamRecord init(int internalTimestamp, String symbol, float price, int quantity,
                            AbstractMap<String,Integer> forwardMap,
                            AbstractMap<Integer,String> backwardMap)
            throws InvalidArgumentException, MissingSymbolException {
        newRecord();

        setSymbolToCode(forwardMap);
        setCodeToSymbol(backwardMap);

        record.putInt(internalTimestamp); // more efficient than using ByeArrayHelper
        Integer code = forwardMap.get(symbol);
        if (code==null) {
            throw new MissingSymbolException(symbol,"","No code found for symbol '"+symbol+"' in market tree (could be because of a new symbol and market tree update is in progress)");
        }
        record.putShort((short)code.intValue()); // more efficient than using ByteArrayHelper
        record.put(ByteArrayHelper.floatToBytes(price, PRICE_FIELD_LENGTH, 100)); // keep it this way as it is safer
        record.putInt(quantity); // more efficient than using ByeArrayHelper
        // last is the insertion timestamp, which we do not know yet
        return this;
    }

    private void newRecord() {
        record = ByteBuffer.allocate(RECORD_LENGTH);
    }

    @SuppressWarnings({"AssignmentToCollectionOrArrayFieldFromParameter"})
    public TickStreamRecord(byte[] bytes, AbstractMap<String,Integer> forwardMap,
                            AbstractMap<Integer,String> backwardMap) {
        record = ByteBuffer.wrap(bytes);
        symbolToCode = forwardMap;
        codeToSymbol = backwardMap;
    }

    /**
     * Returns the raw backing array. Caution! This is not a clone of the array but the actual array.
     * This operation is not only dangerous, but also very not thread safe. It is, however very efficient and creates
     * no extra memory allocation or temporary objects.
     * @return the backing array of the instance
     */
    @Override
    public byte[] toBytes() {
        return record.array();
    }

    @Override
    public ITickStreamRecord replaceBytes(byte[] newValue) throws InvalidArgumentException {
        if (newValue.length != RECORD_LENGTH) {
            throw new InvalidArgumentException("Invalid value. Length must be "+RECORD_LENGTH);
        }

        if (record == null) {
            newRecord();
        }

        for (int i=0;i<RECORD_LENGTH;i++) {
            record.put(i,newValue[i]);
        }

        return this;
    }

    @Override
    public int getTransactionTimestamp() {
        record.position(TRANSACTION_TS_FIELD_OFFSET);
        return record.getInt(); // more efficient as we use a 4 byte int in this implementation
    }

    @Override
    public String getSymbol() {
        record.position(SYMBOL_FIELD_OFFSET);
        return codeToSymbol.get((int)record.getShort()); // more efficient as we use a 2 byte representation of the symbol code
    }

    @Override
    public float getPrice() throws InvalidArgumentException {
        record.position(PRICE_FIELD_OFFSET);
        int intValue = record.getInt();
        return ((float) intValue) / 100;
    }

    @Override
    public int getQuantity() {
        record.position(QUANTITY_FIELD_OFFSET);
        return record.getInt(); // more efficient as we use a 4 byte int in this implementation
    }

    @Override
    public int getInsertionTimestamp() {
        record.position(INSERTION_TS_FIELD_OFFSET);
        return record.getInt(); // more efficient as we use a 4 byte int in this implementation
    }

    /**
     * Sets the insertion timestamp to the internal representation of right now
     */
    @Override
    public void setInsertionTimestamp() {
        int ts = TimeHelper.now();
        setInsertionTimestamp(ts);
    }

    /**
     * Sets the insertion timestamp (useful mainly for testing)
     * @param ts the timestamp to use
     */
    public void setInsertionTimestamp(int ts) {
        record.position(INSERTION_TS_FIELD_OFFSET);
        record.putInt(ts);
    }

    @SuppressWarnings({"AssignmentToCollectionOrArrayFieldFromParameter"})
    public void setSymbolToCode(AbstractMap<String, Integer> symbolToCode) throws InvalidArgumentException {
        if (symbolToCode==null) {
            throw new InvalidArgumentException("forward map may not be null");
        }
        this.symbolToCode = symbolToCode;
    }

    @SuppressWarnings({"AssignmentToCollectionOrArrayFieldFromParameter"})
    public void setCodeToSymbol(AbstractMap<Integer, String> codeToSymbol) throws InvalidArgumentException {
        if (codeToSymbol==null) {
            throw new InvalidArgumentException("backward map may not be null");
        }
        this.codeToSymbol = codeToSymbol;
    }

    public static byte[] symbolToBytes(String symbol, AbstractMap<String,Integer> map) throws InvalidArgumentException {
        Integer code = (Integer)map.get(symbol);
        if (code == null) {
            throw new InvalidArgumentException("Could not find symbol '"+symbol+"' in map!");
        }
        else {
            return ByteArrayHelper.intToBytes(code, SYMBOL_FIELD_LENGTH);
        }
    }

    public static String bytesToSymbol(byte[] bytes, AbstractMap<Integer,String> map) throws InvalidArgumentException {
        if (bytes.length != 2) {
            throw new InvalidArgumentException("The byte array must be of length 2. Got "+bytes.length);
        }

        int key = ByteArrayHelper.byteArrayToInt(bytes);
        String retval = map.get(key);

        if (retval == null) {
            throw new InvalidArgumentException("No corresponding symbol found for code "+key);
        }

        return retval;
    }

    public static String bytesToSymbol(int code, AbstractMap<Integer,String> map) throws InvalidArgumentException {
        return bytesToSymbol(ByteArrayHelper.intToBytes(code,SYMBOL_FIELD_LENGTH), map);
    }

    public static byte[] priceToBytes(float price) throws InvalidArgumentException {
        return ByteArrayHelper.floatToBytes(price, PRICE_FIELD_LENGTH, 100);
    }

    public static float bytesToPrice(byte[] bytes) throws InvalidArgumentException {
        if (bytes.length != PRICE_FIELD_LENGTH) {
            throw new InvalidArgumentException("Incorrect byte array length. Expected "+PRICE_FIELD_LENGTH+" but got "+bytes.length);
        }
        return ByteArrayHelper.bytesToFloat(bytes, 100);
    }

    @Override
    public boolean  equalsWithoutInsertionTs(ITickStreamRecord other) {
        try {
            return (Math.abs(getPrice() - other.getPrice()) < HALF_TICK) &&
                    getQuantity() == other.getQuantity() &&
                    getTransactionTimestamp() == other.getTransactionTimestamp() &&
                    getSymbol().equals(other.getSymbol());
        } catch (InvalidArgumentException e) {
            return false;
        }
    }

    public String toString() {
        try {
            return "Symbol="+getSymbol()+" price="+getPrice()+" quantity="+getQuantity()+" trans.ts="+getTransactionTimestamp()+" insertion.ts="+getInsertionTimestamp();
        } catch (InvalidArgumentException e) {
            return "EXCEPTION!!! "+e;
        }
    }
}
