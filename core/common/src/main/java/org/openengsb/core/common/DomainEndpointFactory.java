/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.core.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openengsb.core.common.context.ContextHolder;
import org.openengsb.core.common.util.OsgiServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * provides utility-methods for retrieving domain-services
 *
 */
public final class DomainEndpointFactory {

    private static Log log = LogFactory.getLog(DomainEndpointFactory.class);

    private static BundleContext bundleContext;

    /**
     * returns the domain-service for the corresponding location in the current context. If no service with that
     * location is found in the current context, the root-context is tried.
     */
    public static <T extends Domain> T getDomainEndpoint(Class<T> domainType, String location) {
        Filter filter = OsgiServiceUtils.getFilterForLocation(domainType, location);
        return OsgiServiceUtils.getOsgiServiceProxy(filter, domainType);
    }

    /**
     * returns domain-services for all domains registered at the given location in the current context and the
     * root-context. If no service is found an empty list is returned.
     */
    public static <T extends Domain> List<T> getDomainEndpoints(Class<T> domainType, String location) {
        return getDomainEndpoints(domainType, location, ContextHolder.get().getCurrentContextId());
    }

    /**
     * returns the domain-service for the corresponding location in the given context. If no service with that location
     * is found in the given context, the root-context is tried.
     */
    public static <T extends Domain> T getDomainEndpoint(Class<T> domainType, String location, String context) {
        Filter filter = OsgiServiceUtils.getFilterForLocation(domainType, location, context);
        return OsgiServiceUtils.getOsgiServiceProxy(filter, domainType);
    }

    /**
     * returns domain-services for all domains registered at the given location in the given context and the
     * root-context. If no service is found an empty list is returned.
     */
    public static <T extends Domain> List<T> getDomainEndpoints(Class<T> domainType, String location, String context) {
        Filter filterForLocation = OsgiServiceUtils.getFilterForLocation(domainType, location);
        ServiceReference[] allServiceReferences;
        try {
            allServiceReferences =
                bundleContext.getAllServiceReferences(domainType.getName(), filterForLocation.toString());
        } catch (InvalidSyntaxException e) {
            // this can never happen, because the filter has been compiled before
            throw new RuntimeException(e);
        }
        List<T> result = new ArrayList<T>();
        if (allServiceReferences == null) {
            log.info("no references found for filter: " + filterForLocation.toString());
            return result;
        }
        log.debug(String.format("found %s references for %s", allServiceReferences.length, filterForLocation));
        for (ServiceReference ref : allServiceReferences) {
            String filterString = String.format("(%s=%s)", Constants.SERVICE_ID, ref.getProperty(Constants.SERVICE_ID));
            try {
                T osgiServiceProxy =
                    OsgiServiceUtils.getOsgiServiceProxy(FrameworkUtil.createFilter(filterString), domainType);
                result.add(osgiServiceProxy);
            } catch (InvalidSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public static void setBundleContext(BundleContext bundleContext) {
        DomainEndpointFactory.bundleContext = bundleContext;
    }

    private DomainEndpointFactory() {
    }

}
