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

package org.openengsb.core.services.internal.deployer.connector;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.matchers.LessThan;
import org.openengsb.core.api.ConnectorInstanceFactory;
import org.openengsb.core.api.ConnectorManager;
import org.openengsb.core.api.OsgiUtilsService;
import org.openengsb.core.api.WiringService;
import org.openengsb.core.api.persistence.ConfigPersistenceService;
import org.openengsb.core.common.OpenEngSBCoreServices;
import org.openengsb.core.common.util.DefaultOsgiUtilsService;
import org.openengsb.core.services.internal.ConnectorManagerImpl;
import org.openengsb.core.services.internal.ConnectorRegistrationManagerImpl;
import org.openengsb.core.services.internal.CorePersistenceServiceBackend;
import org.openengsb.core.services.internal.DefaultConfigPersistenceService;
import org.openengsb.core.services.internal.DefaultWiringService;
import org.openengsb.core.test.AbstractOsgiMockServiceTest;
import org.openengsb.core.test.DummyPersistenceManager;
import org.openengsb.core.test.NullDomain;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class ConnectorDeployerServiceTest extends AbstractOsgiMockServiceTest {

    private ConnectorDeployerService connectorDeployerService;
    private AuthenticationManager authManagerMock;
    private Authentication authMock;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private NullDomain createdService;
    private ConnectorManager serviceManager;
    private String testConnectorData = ""
            + org.openengsb.core.api.Constants.CONNECTOR_KEY + "=a-connector\n"
            + org.openengsb.core.api.Constants.DOMAIN_KEY + "=mydomain\n"
            + org.openengsb.core.api.Constants.ID_KEY + "=service-id\n"
            + "attribute.a-key=a-value";
    private ConnectorInstanceFactory factory;

    @Before
    public void setUp() throws Exception {
        connectorDeployerService = new ConnectorDeployerService();
        authManagerMock = mock(AuthenticationManager.class);
        authMock = mock(Authentication.class);

        ConnectorManagerImpl serviceManagerImpl = new ConnectorManagerImpl();
        ConnectorRegistrationManagerImpl registrationManager = new ConnectorRegistrationManagerImpl();
        registrationManager.setBundleContext(bundleContext);
        serviceManagerImpl.setRegistrationManager(registrationManager);
        this.serviceManager = serviceManagerImpl;

        DummyPersistenceManager dummyPersistenceManager = new DummyPersistenceManager();
        CorePersistenceServiceBackend backend = new CorePersistenceServiceBackend();
        backend.setBundleContext(bundleContext);
        backend.setPersistenceManager(dummyPersistenceManager);
        backend.init();
        DefaultConfigPersistenceService configPersistence = new DefaultConfigPersistenceService(backend);
        Dictionary<String, Object> props2 = new Hashtable<String, Object>();
        props2.put("configuration.id", org.openengsb.core.api.Constants.CONFIG_CONNECTOR);
        registerService(configPersistence, props2, ConfigPersistenceService.class);

        when(authManagerMock.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authMock);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(org.openengsb.core.api.Constants.CONNECTOR_KEY, "a-connector");

        connectorDeployerService.setAuthenticationManager(authManagerMock);
        connectorDeployerService.setServiceManager(serviceManagerImpl);

        factory = mock(ConnectorInstanceFactory.class);
        createdService = mock(NullDomain.class);
        when(factory.createNewInstance(anyString())).thenReturn(createdService);
        registerService(factory, props, ConnectorInstanceFactory.class);

        createDomainProviderMock(NullDomain.class, "mydomain");

        DefaultWiringService defaultWiringService = new DefaultWiringService();
        defaultWiringService.setBundleContext(bundleContext);
        registerServiceViaId(defaultWiringService, "wiring", WiringService.class);
    }

    @Test
    public void testConnectorFiles_shouldBeHandledByDeployer() throws Exception {
        File connectorFile = temporaryFolder.newFile("example.connector");

        assertThat(connectorDeployerService.canHandle(connectorFile), is(true));
    }

    @Test
    public void testUnknownFiles_shouldNotBeHandledByDeplyoer() throws Exception {
        File otherFile = temporaryFolder.newFile("other.txt");

        assertThat(connectorDeployerService.canHandle(otherFile), is(false));
    }

    @Test
    public void testConnectorFile_shouldBeInstalled() throws Exception {
        File connectorFile = createSampleConnectorFile();
        connectorDeployerService.install(connectorFile);

        NullDomain domainEndpoints = OpenEngSBCoreServices.getWiringService().getDomainEndpoint(NullDomain.class, "*");
        domainEndpoints.nullMethod(42);
        verify(createdService).nullMethod(42);
    }

    private File createSampleConnectorFile() throws IOException {
        File connectorFile = temporaryFolder.newFile("example.connector");
        FileUtils.writeStringToFile(connectorFile, testConnectorData);
        return connectorFile;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateConnectorFile_shouldBeUpdated() throws Exception {
        File connectorFile = createSampleConnectorFile();
        connectorDeployerService.install(connectorFile);
        FileUtils.writeStringToFile(connectorFile, testConnectorData + "\nattribute.another=foo");
        connectorDeployerService.update(connectorFile);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> attributeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(factory, times(2)).applyAttributes(eq(createdService), attributeCaptor.capture());
        String value = (String) attributeCaptor.getAllValues().get(1).get("another");
        assertThat(value, is("foo"));
    }

    @Test
    public void testRootService_shouldHaveLowerRanking() throws Exception {
        File connectorFile = new File(temporaryFolder.getRoot() + "/etc/a_root.connector");
        FileUtils.touch(connectorFile);
        FileUtils.writeStringToFile(connectorFile, testConnectorData);

        connectorDeployerService.install(connectorFile);

        ServiceReference reference = bundleContext.getServiceReferences(NullDomain.class.getName(), "")[0];
        String ranking = (String) reference.getProperty(Constants.SERVICE_RANKING);
        assertThat(new Long(ranking), new LessThan<Long>(0L));
    }

    @Test
    public void testNormalService_shouldHaveNoRankingAdded() throws Exception {
        File connectorFile = new File(temporaryFolder.getRoot() + "/config/a_root.connector");
        FileUtils.touch(connectorFile);
        FileUtils.writeStringToFile(connectorFile, testConnectorData);

        connectorDeployerService.install(connectorFile);

        ServiceReference reference = bundleContext.getServiceReferences(NullDomain.class.getName(), "")[0];

        assertThat(reference.getProperty(Constants.SERVICE_RANKING), nullValue());
    }

    @Test
    public void testOverridenRanking_shouldNotBeAltered() throws Exception {
        File connectorFile = new File(temporaryFolder.getRoot() + "/etc/a_root.connector");
        FileUtils.touch(connectorFile);
        FileUtils.writeStringToFile(connectorFile, testConnectorData + "\n"
                + "property." + Constants.SERVICE_RANKING + "=24");

        connectorDeployerService.install(connectorFile);

        ServiceReference ref =
            bundleContext.getServiceReferences(NullDomain.class.getName(),
                String.format("(%s=%s)", Constants.SERVICE_RANKING, 24))[0];
        assertThat(ref, not(nullValue()));
    }

    @Override
    protected void setBundleContext(BundleContext bundleContext) {
        DefaultOsgiUtilsService osgiServiceUtils = new DefaultOsgiUtilsService();
        osgiServiceUtils.setBundleContext(bundleContext);
        registerService(osgiServiceUtils, new Hashtable<String, Object>(), OsgiUtilsService.class);
        OpenEngSBCoreServices.setOsgiServiceUtils(osgiServiceUtils);
    }
}