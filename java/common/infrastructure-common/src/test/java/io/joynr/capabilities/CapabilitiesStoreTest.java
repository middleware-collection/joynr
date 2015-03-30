package io.joynr.capabilities;

/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2014 BMW Car IT GmbH
 * %%
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
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import io.joynr.arbitration.ArbitrationStrategy;
import io.joynr.arbitration.DiscoveryQos;
import io.joynr.endpoints.EndpointAddressBase;
import io.joynr.endpoints.JoynrMessagingEndpointAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import joynr.types.CustomParameter;
import joynr.types.ProviderQos;
import joynr.types.ProviderScope;
import joynr.vehicle.Gps;
import joynr.vehicle.GpsAsync;
import joynr.vehicle.NavigationAsync;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests the interaction of the dispatcher and communication manager.
 */
public class CapabilitiesStoreTest {

    CapabilitiesStore store;

    @Before
    public void setUp() {
        Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                bind(CapabilitiesStore.class).to(CapabilitiesStoreImpl.class);
                bind(CapabilitiesProvisioning.class).to(DefaultCapabilitiesProvisioning.class);

            }
        });
        store = injector.getInstance(CapabilitiesStore.class);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testInvalidEntry() throws Exception {
        ProviderQos providerQos = new ProviderQos();
        providerQos.setScope(ProviderScope.LOCAL);
        CapabilityEntry capabilityEntry = new CapabilityEntry(null,
                                                              GpsAsync.class,
                                                              providerQos,
                                                              "",
                                                              System.currentTimeMillis());
        boolean thrown = false;
        DiscoveryQos discoveryQos = new DiscoveryQos(1000, ArbitrationStrategy.NotSet, 1000);
        try {
            store.add(capabilityEntry);
            store.lookup("hello", GpsAsync.INTERFACE_NAME, discoveryQos.getCacheMaxAge());
        } catch (Exception e) {
            thrown = true;
        }
        Assert.assertTrue(thrown);

    }

    @Test
    public void testMultipleCapEntryRegistrations() {
        String domain = "testDomain";
        String participantId = "testparticipantId";

        ProviderQos providerQos = new ProviderQos(new ArrayList<CustomParameter>(), 1, 0L, ProviderScope.GLOBAL, true);
        EndpointAddressBase endpointAddress = new JoynrMessagingEndpointAddress("testChannel");
        CapabilityEntry capabilityEntry1 = new CapabilityEntry(domain,
                                                               GpsAsync.class,
                                                               providerQos,
                                                               participantId,
                                                               System.currentTimeMillis(),
                                                               endpointAddress);

        CapabilityEntry capabilityEntry2 = new CapabilityEntry(domain,
                                                               NavigationAsync.class,
                                                               providerQos,
                                                               participantId,
                                                               System.currentTimeMillis(),
                                                               endpointAddress);
        store.add(Lists.newArrayList(capabilityEntry1, capabilityEntry2));
        Assert.assertEquals(2, store.getAllCapabilities().size());

    }

    @Test
    public void testCapabilitiesStoreEndPointAddressQuery() {
        testCapabilitiesStoreEndPointAddressQueryInternal(CapabilityScope.LOCALONLY);
        testCapabilitiesStoreEndPointAddressQueryInternal(CapabilityScope.LOCALGLOBAL);
        testCapabilitiesStoreEndPointAddressQueryInternal(CapabilityScope.REMOTE);
    }

    private void testCapabilitiesStoreEndPointAddressQueryInternal(CapabilityScope scope) {
        HashSet<CapabilityEntry> capabilities = store.getAllCapabilities();
        Assert.assertEquals(0, capabilities.size());

        String domain = "testDomain";
        String participantId = "testparticipantId";
        ProviderQos providerQos = new ProviderQos(new ArrayList<CustomParameter>(), 1, 0L, ProviderScope.GLOBAL, true);
        EndpointAddressBase endpointAddress = new JoynrMessagingEndpointAddress("testChannel");
        CapabilityEntry capabilityEntry1 = new CapabilityEntry(domain,
                                                               GpsAsync.class,
                                                               providerQos,
                                                               participantId + "1",
                                                               System.currentTimeMillis(),
                                                               endpointAddress);
        store.add(capabilityEntry1);

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(1, capabilities.size());

        DiscoveryQos discoveryQos = DiscoveryQos.NO_FILTER;

        Collection<CapabilityEntry> newlyEnteredCaps = store.findCapabilitiesForEndpointAddress(endpointAddress,
                                                                                                discoveryQos.getCacheMaxAge());

        Assert.assertEquals(1, newlyEnteredCaps.size());
        Assert.assertEquals(capabilityEntry1, newlyEnteredCaps.iterator().next());

        CapabilityEntry capEntryFake = new CapabilityEntry(domain,
                                                           GpsAsync.class,
                                                           providerQos,
                                                           participantId + "Fake",
                                                           System.currentTimeMillis());
        capEntryFake.addEndpoint(new JoynrMessagingEndpointAddress("testChannelFake"));
        store.add(capEntryFake);

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(2, capabilities.size());

        newlyEnteredCaps = store.findCapabilitiesForEndpointAddress(endpointAddress, discoveryQos.getCacheMaxAge());

        Assert.assertEquals(1, newlyEnteredCaps.size());
        Assert.assertEquals(capabilityEntry1, newlyEnteredCaps.iterator().next());

        store.remove(capEntryFake.getParticipantId());

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(1, capabilities.size());

        CapabilityEntry capabilityEntry2 = new CapabilityEntry(domain,
                                                               NavigationAsync.class,
                                                               providerQos,
                                                               participantId + "2",
                                                               System.currentTimeMillis(),
                                                               endpointAddress);
        store.add(capabilityEntry2);

        // check if newly created Entry overrides old one
        capabilities = store.getAllCapabilities();
        Assert.assertEquals(2, capabilities.size());

        newlyEnteredCaps = store.findCapabilitiesForEndpointAddress(endpointAddress, discoveryQos.getCacheMaxAge());
        Assert.assertEquals(2, newlyEnteredCaps.size());
        Assert.assertTrue(newlyEnteredCaps.contains(capabilityEntry1));
        Assert.assertTrue(newlyEnteredCaps.contains(capabilityEntry2));

        store.remove(Lists.newArrayList(capabilityEntry1.getParticipantId(), capabilityEntry2.getParticipantId()));
        Assert.assertEquals(0, store.getAllCapabilities().size());
        Assert.assertEquals(0, store.findCapabilitiesForEndpointAddress(endpointAddress, discoveryQos.getCacheMaxAge())
                                    .size());
    }

    @Test
    public void testCapabilitiesStoreInterfaceAddressQuery() {
        testCapabilitiesStoreInterfaceAddressQueryInternal(ProviderScope.LOCAL);
        testCapabilitiesStoreInterfaceAddressQueryInternal(ProviderScope.GLOBAL);
    }

    @Test
    public void testCapabilitiesStoreCleanRegisterAndUnregister() {
        HashSet<CapabilityEntry> capabilities = store.getAllCapabilities();
        Assert.assertEquals(0, capabilities.size());

        String domain = "testDomain";
        String participantId = "testparticipantId";
        ProviderQos providerQos = new ProviderQos(new ArrayList<CustomParameter>(), 1, 0L, ProviderScope.LOCAL, true);
        EndpointAddressBase endpointAddress = new JoynrMessagingEndpointAddress("testChannel");
        CapabilityEntry capabilityEntry1 = new CapabilityEntry(domain,
                                                               GpsAsync.class,
                                                               providerQos,
                                                               participantId,
                                                               System.currentTimeMillis(),
                                                               endpointAddress);
        store.add(capabilityEntry1);

        // check that the entry was stored
        CapabilityEntry foundEntryById = store.lookup(participantId, DiscoveryQos.NO_MAX_AGE);
        assertEquals(capabilityEntry1, foundEntryById);

        Collection<CapabilityEntry> foundEntryByDomainInterface = store.lookup(domain, "vehicle/gps");
        assertEquals(1, foundEntryByDomainInterface.size());
        assertEquals(capabilityEntry1, foundEntryByDomainInterface.iterator().next());

        store.remove(capabilityEntry1.getParticipantId());
        // check that the entry has been removed.
        CapabilityEntry notFoundEntryById = store.lookup(participantId, DiscoveryQos.NO_MAX_AGE);
        assertNull(notFoundEntryById);

        Collection<CapabilityEntry> notFoundEntryByDomainInterface = store.lookup(domain, "vehicle/gps");
        assertEquals(0, notFoundEntryByDomainInterface.size());

    }

    private void testCapabilitiesStoreInterfaceAddressQueryInternal(ProviderScope scope) {
        HashSet<CapabilityEntry> capabilities = store.getAllCapabilities();
        Assert.assertEquals(0, capabilities.size());

        String domain = "testDomain";
        String participantId = "testparticipantId";
        ProviderQos providerQos = new ProviderQos(new ArrayList<CustomParameter>(), 1, 0L, scope, true);
        EndpointAddressBase endpointAddress = new JoynrMessagingEndpointAddress("testChannel");
        CapabilityEntry capabilityEntry1 = new CapabilityEntry(domain,
                                                               GpsAsync.class,
                                                               providerQos,
                                                               participantId + "1",
                                                               System.currentTimeMillis(),
                                                               endpointAddress);
        store.add(capabilityEntry1);

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(1, capabilities.size());

        DiscoveryQos discoveryQos = DiscoveryQos.NO_FILTER;

        Collection<CapabilityEntry> newlyEnteredCaps = store.lookup(domain,
                                                                    GpsAsync.INTERFACE_NAME,
                                                                    discoveryQos.getCacheMaxAge());

        Assert.assertEquals(1, newlyEnteredCaps.size());
        Assert.assertEquals(capabilityEntry1, newlyEnteredCaps.iterator().next());

        CapabilityEntry capEntryFake = new CapabilityEntry(domain,
                                                           NavigationAsync.class,
                                                           providerQos,
                                                           participantId,
                                                           System.currentTimeMillis(),
                                                           endpointAddress);

        store.add(capEntryFake);

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(2, capabilities.size());

        newlyEnteredCaps = store.lookup(domain, GpsAsync.INTERFACE_NAME, discoveryQos.getCacheMaxAge());

        Assert.assertEquals(1, newlyEnteredCaps.size());
        Assert.assertEquals(capabilityEntry1, newlyEnteredCaps.iterator().next());

        store.remove(capEntryFake.getParticipantId());

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(1, capabilities.size());

        CapabilityEntry capabilityEntry2 = new CapabilityEntry(domain,
                                                               GpsAsync.class,
                                                               providerQos,
                                                               participantId + "2",
                                                               System.currentTimeMillis());
        capabilityEntry2.addEndpoint(new JoynrMessagingEndpointAddress("testChannel2"));

        store.add(capabilityEntry2);

        // check if newly created Entry overrides old one
        capabilities = store.getAllCapabilities();
        Assert.assertEquals(2, capabilities.size());

        newlyEnteredCaps = store.lookup(domain, GpsAsync.INTERFACE_NAME, discoveryQos.getCacheMaxAge());
        Assert.assertEquals(2, newlyEnteredCaps.size());
        Assert.assertTrue(newlyEnteredCaps.contains(capabilityEntry1));
        Assert.assertTrue(newlyEnteredCaps.contains(capabilityEntry2));

        store.remove(Lists.newArrayList(capabilityEntry1.getParticipantId(), capabilityEntry2.getParticipantId()));
        Assert.assertEquals(0, store.getAllCapabilities().size());
        Assert.assertEquals(0, store.findCapabilitiesForEndpointAddress(endpointAddress, discoveryQos.getCacheMaxAge())
                                    .size());
    }

    @Test
    public void testAddCapabilityEntryWithSameDomainInterfaceNameParticipantIdremovesOldEntryScopeLocal() {
        testAddCapabilityEntryWithSameDomainInterfaceNameParticipantIdremovesOldEntryInternal(ProviderScope.LOCAL);
    }

    @Test
    public void testAddCapabilityEntryWithSameDomainInterfaceNameParticipantIdremovesOldEntryScopeGlobal() {
        testAddCapabilityEntryWithSameDomainInterfaceNameParticipantIdremovesOldEntryInternal(ProviderScope.GLOBAL);
    }

    private void testAddCapabilityEntryWithSameDomainInterfaceNameParticipantIdremovesOldEntryInternal(ProviderScope scope) {
        HashSet<CapabilityEntry> capabilities = store.getAllCapabilities();
        Assert.assertEquals(0, capabilities.size());

        String domain = "testDomain";
        String participantId = "testparticipantId";
        ProviderQos providerQos = new ProviderQos(new ArrayList<CustomParameter>(), 1, 0L, scope, true);
        EndpointAddressBase endpointAddress = new JoynrMessagingEndpointAddress("testChannel");
        CapabilityEntry capabilityEntry1 = new CapabilityEntry(domain,
                                                               GpsAsync.class,
                                                               providerQos,
                                                               participantId,
                                                               System.currentTimeMillis(),
                                                               endpointAddress);
        store.add(capabilityEntry1);

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(1, capabilities.size());

        DiscoveryQos discoveryQos = DiscoveryQos.NO_FILTER;

        Collection<CapabilityEntry> newlyEnteredCaps = store.lookup(domain,
                                                                    Gps.INTERFACE_NAME,
                                                                    discoveryQos.getCacheMaxAge());
        Assert.assertEquals(1, newlyEnteredCaps.size());
        Assert.assertEquals(capabilityEntry1, newlyEnteredCaps.iterator().next());

        CapabilityEntry newEnteredCapability = store.lookup(participantId, discoveryQos.getCacheMaxAge());
        Assert.assertEquals(capabilityEntry1, newEnteredCapability);

        CapabilityEntry capEntryFake = new CapabilityEntry(domain,
                                                           GpsAsync.class,
                                                           providerQos,
                                                           participantId + "Fake",
                                                           System.currentTimeMillis());
        capEntryFake.addEndpoint(new JoynrMessagingEndpointAddress("testChannelFake"));
        store.add(capEntryFake);

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(2, capabilities.size());

        store.remove(capEntryFake.getParticipantId());

        capabilities = store.getAllCapabilities();
        Assert.assertEquals(1, capabilities.size());

        EndpointAddressBase endpointAddress2 = new JoynrMessagingEndpointAddress("testChannelOverride");
        CapabilityEntry capabilityEntry2 = new CapabilityEntry(domain,
                                                               GpsAsync.class,
                                                               providerQos,
                                                               participantId,
                                                               System.currentTimeMillis(),
                                                               endpointAddress2);
        store.add(capabilityEntry2);

        // check if newly created Entry overrides old one
        capabilities = store.getAllCapabilities();
        Assert.assertEquals(1, capabilities.size());

        newlyEnteredCaps = store.lookup(domain, Gps.INTERFACE_NAME, discoveryQos.getCacheMaxAge());
        Assert.assertEquals(1, newlyEnteredCaps.size());
        Assert.assertEquals(capabilityEntry2, newlyEnteredCaps.iterator().next());

        CapabilityEntry newlyEnteredCapability = store.lookup(participantId, discoveryQos.getCacheMaxAge());
        Assert.assertEquals(capabilityEntry2, newlyEnteredCapability);
    }
}
