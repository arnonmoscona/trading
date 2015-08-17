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
