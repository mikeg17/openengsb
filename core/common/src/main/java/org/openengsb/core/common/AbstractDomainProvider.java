/**

   Copyright 2010 OpenEngSB Division, Vienna University of Technology

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.openengsb.core.common;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.openengsb.core.common.util.BundleStrings;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;

/**
 * Base class for {@code DomainProvider} implementations with the following
 * unctionality:
 * <ul>
 * <li>extracts domain interface through parameterized type</li>
 * <li>id is class name of domain interface</li>
 * <li>name is looked up through localized
 * {@code BundleStrings.getString("domain.name")}</li>
 * <li>description is looked up through localized
 * {@code BundleStrings.getString("domain.description")}</li>
 * <li>returns an empty event list</li>
 * </ul>
 */
public abstract class AbstractDomainProvider<DomainType extends Domain> implements DomainProvider, BundleContextAware {

    private BundleContext bundleContext;
    private BundleStrings strings;
    private final Class<DomainType> domainInterface;

    @SuppressWarnings("unchecked")
    public AbstractDomainProvider() {
        domainInterface = (Class<DomainType>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    @Override
    public String getId() {
        return domainInterface.getName();
    }

    @Override
    public String getName() {
        return getName(Locale.getDefault());
    }

    @Override
    public String getName(Locale locale) {
        return strings.getString("domain.name", locale);
    }

    @Override
    public String getDescription() {
        return getDescription(Locale.getDefault());
    }

    @Override
    public String getDescription(Locale locale) {
        return strings.getString("domain.description", locale);
    }

    @Override
    public Class<DomainType> getDomainInterface() {
        return domainInterface;
    }

    @Override
    public List<Class<? extends Event>> getEvents() {
        return new ArrayList<Class<? extends Event>>();
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.strings = new BundleStrings(this.bundleContext.getBundle());
    }

    protected BundleContext getBundleContext() {
        return bundleContext;
    }
}