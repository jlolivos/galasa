/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.framework.internal.cts;

import javax.validation.constraints.NotNull;

import org.osgi.service.component.annotations.Component;

import dev.galasa.framework.spi.ConfidentialTextException;
import dev.galasa.framework.spi.IConfidentialTextServiceRegistration;
import dev.galasa.framework.spi.IFrameworkInitialisation;

@Component(service = { IConfidentialTextServiceRegistration.class })
public class FrameworkConfidentialTextServiceRegistration implements IConfidentialTextServiceRegistration {

    /**
     * This method intialises the service with the framework, managers can then
     * access this service.
     * 
     * @param IFrameworkInitialisation - the framework setup.
     * @throws ConfidentialTextException
     */
    public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation) throws ConfidentialTextException {
        try {
            frameworkInitialisation.registerConfidentialTextService(new FrameworkConfidentialTextService());
        } catch (ConfidentialTextException e) {
            throw e;
        }
    }
}
