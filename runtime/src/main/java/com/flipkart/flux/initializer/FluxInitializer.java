/*
 * Copyright 2012-2016, the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.flux.initializer;

import com.flipkart.flux.MigrationUtil.MigrationsRunner;
import com.flipkart.flux.guice.module.ConfigModule;
import com.flipkart.flux.guice.module.FluxServletModule;
import com.flipkart.flux.guice.module.HibernateModule;
import com.flipkart.polyguice.core.support.Polyguice;
import com.google.inject.servlet.GuiceFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * <code>FluxInitializer</code> is the initializer class which starts jetty server and loads polyguice containers.
 * @author shyam.akirala
 */
public class FluxInitializer {

    private static final Logger logger = LogManager.getLogger(FluxInitializer.class);

    private static Server server;

    private static Polyguice fluxRuntimeContainer;

    @Inject
    private static MigrationsRunner migrationsRunner;

    public static void main(String[] args) throws Exception {
        if(args != null && args.length > 0) {
            String param = args[0];
            switch (param) {
                case "start" :
                    start();
                    break;
                case "stop" :
                    stop();
                    break;
                case "migrate" :
                    migrate();
                    break;
                default:
                    logger.error("Incorrect parameter has passed. Valid parameters are start | stop | migrate");
                    break;
            }
        }
    }

    private static void loadFluxRuntimeContainer() {
        logger.debug("loading flux runtime container");
        fluxRuntimeContainer = new Polyguice();
        fluxRuntimeContainer.modules(new ConfigModule(), new HibernateModule(), new FluxServletModule());
        fluxRuntimeContainer.prepare();
    }

    public static void start() throws Exception {
        if(server != null && server.isRunning()) {
            logger.error("Server is already started");
        } else {
            //load flux runtime container
            loadFluxRuntimeContainer();
            logger.debug("starting jetty server");
            server = new Server(9999);
            ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
            context.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
            context.addServlet(DefaultServlet.class, "/*");
            server.setStopAtShutdown(true);
            server.setStopTimeout(30 * 1000); //Max time to wait for Jetty, to cleanly shutdown before forcibly terminating.
            server.start();
        }
    }

    public static void stop() throws Exception {
        //STOP THE JETTY SERVER
    }

    public static void migrate() {
        loadFluxRuntimeContainer();
        //THIS DOESN'T WORK, AS IT IS STATIC, FIND A WAY TO INJECT
        migrationsRunner.migrate();
    }
}
