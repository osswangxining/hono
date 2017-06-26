/**
 * Copyright (c) 2016, 2017 Red Hat and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat - initial creation
 *    Bosch Software Innovations GmbH - extend AbtractApplication
 */

package org.eclipse.hono.streaming.analysis;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The Hono Streaming Analysis main application class.
 */
@ComponentScan(basePackages = "org.eclipse.hono.streaming.analysis")
@Configuration
@EnableAutoConfiguration
@SpringBootApplication
public class Application {
  private static Logger logger = LoggerFactory.getLogger(Application.class);

  @Bean
  protected ServletContextListener listener() {
    return new ServletContextListener() {

      @Override
      public void contextInitialized(ServletContextEvent sce) {
        logger.info("ServletContext initialized");
      }

      @Override
      public void contextDestroyed(ServletContextEvent sce) {
        logger.info("ServletContext destroyed");
      }

    };
  }

  /**
   * Starts the Streaming Analysis application.
   * 
   * @param args
   *          Command line args passed to the application.
   */
  public static void main(final String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
