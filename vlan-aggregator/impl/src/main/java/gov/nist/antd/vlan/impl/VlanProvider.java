/*
 * This program and accompanying materials are made available to the public domain
 * without copyright in accordance with the following disclaimer:
 *
 *
 */
package gov.nist.antd.vlan.impl;

import java.util.Collection;
import java.util.HashMap;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.nist.params.xml.ns.yang.nist.network.topology.rev170915.Links;
import org.opendaylight.yang.gen.v1.urn.nist.params.xml.ns.yang.nist.network.topology.rev170915.Topology;
import org.opendaylight.yang.gen.v1.urn.nist.params.xml.ns.yang.nist.network.topology.rev170915.TrunkSwitches;
import org.opendaylight.yang.gen.v1.urn.nist.params.xml.ns.yang.nist.network.topology.rev170915.Trunks;
import org.opendaylight.yang.gen.v1.urn.nist.params.xml.ns.yang.nist.network.topology.rev170915.links.Link;
import org.opendaylight.yang.gen.v1.urn.nist.params.xml.ns.yang.nist.network.topology.rev170915.trunks.Forwarders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("ucd")
public class VlanProvider {

    private static final Logger LOG = LoggerFactory
            .getLogger(VlanProvider.class);

    private PacketProcessingService packetProcessingService;

    private final DataBroker dataBroker;

    private FlowCommitWrapper flowCommitWrapper;

    private HashMap<String, InstanceIdentifier<FlowCapableNode>> uriToNodeMap = new HashMap<>();


    private NotificationService notificationService;

    private RpcProviderRegistry rpcProviderRegistry;

    private gov.nist.antd.vlan.impl.TopologyDataStoreListener topoDataStoreListener;

    private WakeupOnFlowCapableNode wakeupListener;

    private ListenerRegistration<WakeupOnFlowCapableNode> wakeupOnFlowCapableNodeRegistration;

    private OdlInterfaceRpcService ifMRpcService;

    private Trunks trunks;

    private Links links;

    private TrunkConfigDataStoreListener trunksDataStoreListener;

    private static InstanceIdentifier<Topology> getTopologyWildCardPath() {
        return InstanceIdentifier.create(Topology.class);
    }

    private static InstanceIdentifier<FlowCapableNode> getFlowCapableNodeWildcardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class);

    }

    private static InstanceIdentifier<TrunkSwitches> getTrunksWildCardPath() {
        return InstanceIdentifier.create(TrunkSwitches.class);
    }

    @SuppressWarnings("ucd")
    public VlanProvider(final DataBroker dataBroker,
            PacketProcessingService packetProcessingService,
            RpcProviderRegistry rpcProviderRegistry,
            NotificationService notificationService) {
        assert dataBroker != null;
        assert packetProcessingService != null;
        assert notificationService != null;
        assert rpcProviderRegistry != null;

        this.dataBroker = dataBroker;
        this.packetProcessingService = packetProcessingService;
        this.flowCommitWrapper = new FlowCommitWrapper(dataBroker);
        this.notificationService = notificationService;
        this.rpcProviderRegistry = rpcProviderRegistry;

    }

    /**
     * Method called when the blueprint container is created.
     */
    @SuppressWarnings("ucd")
    public void init() {
        LOG.info("VlanProvider Session Initiated");


        InstanceIdentifier<Topology> topoWildCardPath = getTopologyWildCardPath();
        final DataTreeIdentifier<Topology> topoId = new DataTreeIdentifier<Topology>(
                LogicalDatastoreType.CONFIGURATION, topoWildCardPath);
        this.topoDataStoreListener = new TopologyDataStoreListener(this);
        this.dataBroker.registerDataTreeChangeListener(topoId,
                topoDataStoreListener);

        final DataTreeIdentifier<TrunkSwitches> trunkDtI = new DataTreeIdentifier<TrunkSwitches> (
                LogicalDatastoreType.CONFIGURATION, getTrunksWildCardPath());
        this.trunksDataStoreListener = new TrunkConfigDataStoreListener(this);
        this.dataBroker.registerDataTreeChangeListener(trunkDtI,
                trunksDataStoreListener);

        this.ifMRpcService = rpcProviderRegistry
                .getRpcService(OdlInterfaceRpcService.class);

        PacketProcessingListenerImpl packetInDispatcher = new PacketProcessingListenerImpl(
                this);
        ListenerRegistration<PacketProcessingListenerImpl> registration = notificationService
                .registerNotificationListener(packetInDispatcher);
        this.wakeupListener = new WakeupOnFlowCapableNode(this);
        final DataTreeIdentifier<FlowCapableNode> dataTreeIdentifier = new DataTreeIdentifier<FlowCapableNode>(
                LogicalDatastoreType.OPERATIONAL, getFlowCapableNodeWildcardPath());
        this.wakeupOnFlowCapableNodeRegistration = this.dataBroker
                .registerDataTreeChangeListener(dataTreeIdentifier,
                        wakeupListener);

    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    @SuppressWarnings("ucd")
    public void close() {
        this.wakeupOnFlowCapableNodeRegistration.close();
        LOG.info("VlanProvider Closed");
    }

    PacketProcessingService getPacketProcessingService() {
        return this.packetProcessingService;
    }

    FlowCommitWrapper getFlowCommitWrapper() {
        return this.flowCommitWrapper;
    }

    void putInUriToNodeMap(String nodeUri,
            InstanceIdentifier<FlowCapableNode> nodePath) {
        this.uriToNodeMap.put(nodeUri, nodePath);
    }

    void removeFromUriToNodeMap(String nodeUri) {
        this.uriToNodeMap.remove(nodeUri);
    }

    synchronized InstanceIdentifier<FlowCapableNode> getNode(String nodeUri) {
        return uriToNodeMap.get(nodeUri);
    }

    synchronized Collection<InstanceIdentifier<FlowCapableNode>> getNodes() {
        return uriToNodeMap.values();
    }

    boolean isNpeSwitch(String nodeUri) {
        if (this.trunks != null) {
            for (Forwarders link : trunks.getForwarders()) {
                if (link.getSwitchId().getValue().equals(nodeUri))
                    return true;
            }
        }
        return false;
    }

    boolean isCpeNode(String nodeId) {
        if ( this.links != null) {
            for( Link link : this.links.getLink()) {
                if ( link.getCpeSwitch().getValue().equals(nodeId)) {
                    return true;
                }
            }
        }
        LOG.info("isCpeNode " + nodeId + " is false");
        return false;
    }

    int getCpeTag(String nodeId) {
        if (this.trunks != null) {
            for( Link link : this.links.getLink()) {
                if ( link.getCpeSwitch().getValue().equals(nodeId)) {
                    return link.getVlan();
                }
            }
        }
        return -1;
    }

    public OdlInterfaceRpcService getInterfaceManagerRpcService() {
        return this.ifMRpcService;
    }



    public String getTrunkInterface(String switchId, int vlanTag) {
        if (trunks != null) {
            for (Forwarders npeSwitch : trunks.getForwarders()) {
                if (npeSwitch.getSwitchId().getValue().equals(switchId)) {
                    for (int vlanId : npeSwitch.getVlans()) {
                        if (vlanId == vlanTag) {
                            return npeSwitch.getTrunkInterface();
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getUplinkInterface(String switchId) {
        if (links != null) {
            for ( Link link: this.links.getLink()) {
                if ( link.getCpeSwitch().getValue().equals(switchId)) {
                    return link.getUplinkInterface();
                }
            }
        }
        return null;
    }

    public void setTrunks(Trunks trunks) {
        this.trunks = trunks;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    WakeupOnFlowCapableNode  getWakeupListener() {
        return this.wakeupListener;
    }

    public boolean isConfigured() {
        return  this.links != null && this.trunks != null;
    }

}
