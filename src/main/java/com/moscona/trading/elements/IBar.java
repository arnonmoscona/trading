package com.moscona.trading.elements;

import com.moscona.exceptions.InvalidArgumentException;

/**
 * Created by Arnon on 5/5/2014.
 */
public interface IBar {
    void set(int openCents, int closeCents, int highCents, int lowCents, int volume) throws InvalidArgumentException;

    int getOpenCents();

    void setOpenCents(int openCents);

    int getCloseCents();

    void setCloseCents(int closeCents);

    int getHighCents();

    void setHighCents(int highCents);

    int getLowCents();

    void setLowCents(int lowCents);

    int getVolume();

    void setVolume(int volume);

    float getOpen();

    void setOpen(float open);

    float getClose();

    void setClose(float close);

    float getHigh();

    void setHigh(float high);

    float getLow();

    void setLow(float low);
}
