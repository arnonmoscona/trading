package com.moscona.trading.excptions;

import com.moscona.util.ExceptionHelper;

/**
 * Created by Arnon on 5/5/2014.
 */
public class MissingSymbolException extends Exception {
    private static final long serialVersionUID = -44610556472025504L;
    String symbol;
    String literalError;
    public MissingSymbolException(String symbol, String literalError, String message) {
        super("["+symbol+" : "+literalError+"] "+message);
        this.symbol = symbol;
        this.literalError = literalError;
    }
    public MissingSymbolException(String symbol, String literalError, String message, Throwable cause) {
        super(message, cause);
        this.symbol = symbol;
        this.literalError = literalError;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getLiteralError() {
        return literalError;
    }

    /**
     * Looks into the exception and its causes to find the first cause that is a MissingSymbolException
     * @param exception
     * @param maxCauseDepth the maximum depth to search (to avoid infinite looping)
     * @return the first MissingSymbolException found or null
     */
    public static MissingSymbolException fishOutOf(Throwable exception, int maxCauseDepth) {
        return (MissingSymbolException) ExceptionHelper.fishOutOf(exception, MissingSymbolException.class, maxCauseDepth);
    }
}
