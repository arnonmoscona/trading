package com.moscona.trading.elements;

import com.moscona.exceptions.InvalidArgumentException;

/**
 * Created: Apr 5, 2010 11:19:31 AM By: Arnon Moscona The most basic version of a bar value object. Used either by
 * itself, where symbols and time are inferred from context (each the structure it is isn) or when using subclasses that
 * have symbol and/or time information added.
 */
public class Bar implements IBar {
    public enum BarField {open, high, low, close}

    protected int openCents;
    protected int closeCents;
    protected int highCents;
    protected int lowCents;
    protected int volume;

    public Bar(int openCents, int closeCents, int highCents, int lowCents, int volume) throws InvalidArgumentException {
        init(openCents, closeCents, highCents, lowCents, volume);
    }

    @SuppressWarnings({"OverridableMethodCallDuringObjectConstruction"})
    public Bar(float open, float close, float high, float low, int volume) throws InvalidArgumentException {
        init(asInt(open), asInt(close), asInt(high), asInt(low), volume);
    }

    private void init(int openCents, int closeCents, int highCents, int lowCents, int volume) throws InvalidArgumentException {
        this.openCents = openCents;
        this.closeCents = closeCents;
        this.highCents = highCents;
        this.lowCents = lowCents;
        this.volume = volume;

        validate();
    }

    /**
     * Used to validate the state of the object *after* making changes. If not valid will throw and exception. After an
     * exception is thrown the object is *not* restored to a valid state and must be fixed by the caller.
     *
     * @throws InvalidArgumentException if the numbers make no sense
     */
    protected void validate() throws InvalidArgumentException {  // need spec
        // validate ranges
        if (openCents < lowCents)
            throw new InvalidArgumentException("Open price is lower then low price.");
        if (closeCents < lowCents)
            throw new InvalidArgumentException("Close price is lower then low price.");
        if (openCents > highCents)
            throw new InvalidArgumentException("Open price is higher then high price.");
        if (closeCents > highCents)
            throw new InvalidArgumentException("Close price is higher then high price.");
        // everything must be positive
        if (openCents < 0 || closeCents < 0 || lowCents < 0 || highCents < 0)
            throw new InvalidArgumentException("All prices must be positive. At least one is negative");
        if (volume < 0 && anyValueNonZero())
            throw new InvalidArgumentException("The volume must be positive.");
    }

    protected boolean anyValueNonZero() {
        return openCents != 0 || lowCents != 0 || highCents != 0 || closeCents != 0 || volume != 0;
    }

    @Override
    public void set(int openCents, int closeCents, int highCents, int lowCents, int volume) throws InvalidArgumentException {
        init(openCents, closeCents, highCents, lowCents, volume);
    }

    @Override
    public int getOpenCents() {
        return openCents;
    }

    @Override
    public void setOpenCents(int openCents) {
        this.openCents = openCents;
    }

    @Override
    public int getCloseCents() {
        return closeCents;
    }

    @Override
    public void setCloseCents(int closeCents) {
        this.closeCents = closeCents;
    }

    @Override
    public int getHighCents() {
        return highCents;
    }

    @Override
    public void setHighCents(int highCents) {
        this.highCents = highCents;
    }

    @Override
    public int getLowCents() {
        return lowCents;
    }

    @Override
    public void setLowCents(int lowCents) {
        this.lowCents = lowCents;
    }

    @Override
    public int getVolume() {
        return volume;
    }

    @Override
    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Override
    public float getOpen() {
        return asFloat(openCents);
    }

    @Override
    public void setOpen(float open) {
        this.openCents = asInt(open);
    }

    @Override
    public float getClose() {
        return asFloat(closeCents);
    }

    @Override
    public void setClose(float close) {
        this.closeCents = asInt(close);
    }

    @Override
    public float getHigh() {
        return asFloat(highCents);
    }

    @Override
    public void setHigh(float high) {
        this.highCents = asInt(high);
    }

    @Override
    public float getLow() {
        return asFloat(lowCents);
    }

    @Override
    public void setLow(float low) {
        this.lowCents = asInt(low);
    }

    protected float asFloat(int cents) {
        return ((float) cents) / 100.0f;
    }

    protected int asInt(float price) {
        return Math.round(price * 100.0f);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        return completeJsonString(toJsonStringFragment(builder).toString());
    }

    protected String completeJsonString(String jsonStringFragment) {
        return "{" + jsonStringFragment + "}";
    }

    protected StringBuilder toJsonStringFragment(StringBuilder builder) {
        builder.append("open:").append(getOpen());
        builder.append(",high:").append(getHigh());
        builder.append(",low:").append(getLow());
        builder.append(",close:").append(getClose());
        builder.append(",volume:").append(getVolume());

        return builder;
    }

    public boolean hasAnyZeroPriceValue() {
        return openCents == 0 || highCents == 0 || lowCents == 0 || closeCents == 0;
    }

    public boolean hasNonZeroPrices() {
        return openCents > 0 || highCents > 0 || lowCents > 0 || closeCents > 0;
    }

    public float get(BarField field) throws InvalidArgumentException {
        switch (field) {
            case open:
                return getOpen();
            case close:
                return getClose();
            case high:
                return getHigh();
            case low:
                return getLow();
        }
        throw new InvalidArgumentException("Cannot extract bar field " + field);
    }
}

