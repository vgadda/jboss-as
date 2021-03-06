/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.service;

import static org.jboss.as.jpa.JpaLogger.ROOT_LOGGER;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.transaction.TransactionUtil;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * represents the global JPA Service
 *
 * @author Scott Marlow
 */
public class JPAService implements Service<Void> {

    public static final ServiceName SERVICE_NAME = JPAServiceNames.getJPAServiceName();

    private static volatile String defaultDataSourceName = null;
    private static volatile ExtendedPersistenceInheritance defaultExtendedPersistenceInheritance = null;
    private static volatile boolean defaultVFS = true;

    public static String getDefaultDataSourceName() {
        ROOT_LOGGER.tracef("JPAService.getDefaultDataSourceName() == %s", JPAService.defaultDataSourceName);
        return defaultDataSourceName;
    }

    public static void setDefaultDataSourceName(String dataSourceName) {
        ROOT_LOGGER.tracef("JPAService.setDefaultDataSourceName(%s), previous value = %s", dataSourceName, JPAService.defaultDataSourceName);
        defaultDataSourceName = dataSourceName;
    }

    public static ExtendedPersistenceInheritance getDefaultExtendedPersistenceInheritance() {
        ROOT_LOGGER.tracef("JPAService.getDefaultExtendedPersistenceInheritance() == %s", defaultExtendedPersistenceInheritance.toString());
        return defaultExtendedPersistenceInheritance;
    }

    public static void setDefaultExtendedPersistenceInheritance(ExtendedPersistenceInheritance defaultExtendedPersistenceInheritance) {
        ROOT_LOGGER.tracef("JPAService.setDefaultExtendedPersistenceInheritance(%s)", defaultExtendedPersistenceInheritance.toString());
        JPAService.defaultExtendedPersistenceInheritance = defaultExtendedPersistenceInheritance;
    }

    public static boolean isDefaultVFS() {
        ROOT_LOGGER.tracef("JPAService.isDefaultVFS() == %b", defaultVFS);
        return defaultVFS;
    }

    public static void setDefaultVFS(boolean defaultVFS) {
        ROOT_LOGGER.tracef("JPAService.setDefaultVFS(%b), previous value == %b", defaultVFS, JPAService.defaultVFS);
        JPAService.defaultVFS = defaultVFS;
    }

    public static ServiceController<?> addService(
            final ServiceTarget target,
            final String defaultDataSourceName,
            final ExtendedPersistenceInheritance defaultExtendedPersistenceInheritance,
            final boolean defaultVFS,
            final ServiceListener<Object>... listeners) {
        JPAService jpaService = new JPAService();
        setDefaultDataSourceName(defaultDataSourceName);
        setDefaultExtendedPersistenceInheritance(defaultExtendedPersistenceInheritance);
        setDefaultVFS(defaultVFS);

        // set the transaction manager to be accessible via TransactionUtil
        final Injector<TransactionManager> transactionManagerInjector =
            new Injector<TransactionManager>() {
                public void inject(final TransactionManager value) throws InjectionException {
                    TransactionUtil.setTransactionManager(value);
                }

                public void uninject() {
                    // injector.uninject();
                }
            };
        // set the transaction service registry to be accessible via TransactionUtil (after service is installed below)
        final Injector<TransactionSynchronizationRegistry> transactionRegistryInjector =
            new Injector<TransactionSynchronizationRegistry>() {
                public void inject(final TransactionSynchronizationRegistry value) throws
                    InjectionException {
                    TransactionUtil.setTransactionSynchronizationRegistry(value);
                }

                public void uninject() {
                    // injector.uninject();
                }
            };

        return target.addService(SERVICE_NAME, jpaService)
            .addListener(listeners)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .addDependency(TransactionManagerService.SERVICE_NAME, new CastingInjector<TransactionManager>(transactionManagerInjector, TransactionManager.class))
            .addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, new CastingInjector<TransactionSynchronizationRegistry>(transactionRegistryInjector, TransactionSynchronizationRegistry.class))
            .addDependency(JPAUserTransactionListenerService.SERVICE_NAME)
            .install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {

    }

    @Override
    public void stop(StopContext stopContext) {

    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

}
