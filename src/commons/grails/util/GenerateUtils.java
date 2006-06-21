/* Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.spring.SpringConfig;
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator;
import org.springframework.binding.support.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springmodules.beans.factory.drivers.xml.XmlApplicationContextDriver;
import groovy.lang.GroovyClassLoader;

/**
 * Utility class for generating Grails artifacts likes views, controllers etc.
 *
 * @author Graeme Rocher
 * @since 10-Feb-2006
 */
public class GenerateUtils {

    private static Log LOG = LogFactory.getLog(RunTests.class);
    private static final String VIEWS = "view";
    private static final String CONTROLLER = "controller";
	private static final String ALL = "all";

    public static void main(String[] args) throws Exception {
        if(args.length < 2)
            return;

        String type = args[0];
        String domainClassName = args[1];



        ApplicationContext parent = new ClassPathXmlApplicationContext("applicationContext.xml");
        GrailsApplication application = (DefaultGrailsApplication)parent.getBean("grailsApplication", DefaultGrailsApplication.class);

        GrailsDomainClass domainClass = getDomainCallFromApplication(application,domainClassName);

        // bootstrap application to try hibernate domain classes
        if(domainClass == null) {
            SpringConfig config = new SpringConfig(application);
            ConfigurableApplicationContext appCtx = (ConfigurableApplicationContext)
                new XmlApplicationContextDriver().getApplicationContext(
                    config.getBeanReferences(), parent);
            Assert.notNull(appCtx);
        }

        // retry
        domainClass = getDomainCallFromApplication(application,domainClassName);
        if(domainClass == null) {
            LOG.debug("Unable to generate ["+type+"] domain class not found for name ["+domainClassName+"]");
            return;
        }

        GroovyClassLoader gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());

        GrailsTemplateGenerator generator = (GrailsTemplateGenerator)gcl.parseClass(gcl.getResourceAsStream("org/codehaus/groovy/grails/scaffolding/DefaultGrailsTemplateGenerator.groovy"))
                                                                            .newInstance();

        if(VIEWS.equals(type) || ALL.equals(type)) {
            LOG.info("Generating views for domain class ["+domainClass.getName()+"]");
            generator.generateViews(domainClass,".");
        }
        else if(CONTROLLER.equals(type)|| ALL.equals(type)) {
           LOG.info("Generating controller for domain class ["+domainClass.getName()+"]");
           generator.generateController(domainClass,".");
        }
        else {
            LOG.info("Grails was unable to generate templates for unsupported type ["+type+"]");
        }
    }

    private static GrailsDomainClass getDomainCallFromApplication(GrailsApplication application, String domainClassName) {
        GrailsDomainClass domainClass = application.getGrailsDomainClass(domainClassName);
        if(domainClass == null) {
            domainClass = application.getGrailsDomainClass(domainClassName.substring(0,1).toUpperCase() + domainClassName.substring(1));
        }
        return domainClass;
    }
}
