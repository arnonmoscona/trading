package com.moscona.trading;

/**
 * Created: Apr 27, 2010 2:50:06 PM
 * By: Arnon Moscona
 * An interface for classes that have a service bundle attribute
 */
public interface IServicesClient {
    public ServicesBundle getServicesBundle();
    public void setServicesBundle(ServicesBundle services);
}
