package gov.nist.antd.ids.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WakeupOnFlowCapableNode implements DataTreeChangeListener<FlowCapableNode> {
	private static final Logger LOG = LoggerFactory.getLogger(WakeupOnFlowCapableNode.class);

	private IdsProvider idsProvider;

	private ArrayList<InstanceIdentifier<FlowCapableNode>> pendingNodes;

	// PacketInDispatcher(String nodeId, InstanceIdentifier<FlowCapableNode>
	// node,
	// IMdsalApiManager mdsalApiManager,
	// FlowCommitWrapper flowCommitWrapper, idsProvider idsProvider)
	//
	public WakeupOnFlowCapableNode(IdsProvider idsProvider) {
		this.idsProvider = idsProvider;
	}

	@Override
	public void onDataTreeChanged(Collection<DataTreeModification<FlowCapableNode>> modifications) {
		LOG.debug("WakeupOnFlowCapableNode: onDataTreeChanged");

		for (DataTreeModification<FlowCapableNode> modification : modifications) {
			if (modification.getRootNode().getModificationType() == ModificationType.WRITE) {
				LOG.info("got a WRITE modification");
				InstanceIdentifier<FlowCapableNode> ii = modification.getRootPath().getRootIdentifier();
				onFlowCapableSwitchAppeared(ii);
			} else if (modification.getRootNode().getModificationType() == ModificationType.DELETE) {
				LOG.info("Got a DELETE modification");
				InstanceIdentifier<FlowCapableNode> ii = modification.getRootPath().getRootIdentifier();
				onFlowCapableSwitchDisappeared(ii);
			} else {
				LOG.debug("WakeupOnFlowCapableNode : " + modification.getRootNode().getModificationType());
			}
		}

	}

	/**
	 * Flow to send the IDS HELLO to the controller if it has not already been
	 * installed.
	 * 
	 * @param nodeUri
	 * @param node
	 */
	private synchronized void installSendIdsHelloToControllerFlow(String nodeUri,
			InstanceIdentifier<FlowCapableNode> node) {
		if (!idsProvider.getFlowCommitWrapper().flowExists(nodeUri + ":", SdnMudConstants.FIRST_TABLE, node)) {
			FlowId flowId = InstanceIdentifierUtils.createFlowId(nodeUri + ":ids");
			FlowCookie flowCookie = SdnMudConstants.IDS_REGISTRATION_FLOW_COOKIE;
			LOG.info("IDS_REGISTRATION_FLOW_COOKIE " + flowCookie.getValue().toString(16));
			FlowBuilder fb = FlowUtils.createDestIpMatchSendToController(SdnMudConstants.IDS_REGISTRATION_ADDRESS,
					SdnMudConstants.IDS_REGISTRATION_PORT, SdnMudConstants.FIRST_TABLE, flowCookie, flowId,
					SdnMudConstants.IDS_REGISTRATION_METADATA);
			idsProvider.getFlowCommitWrapper().writeFlow(fb, node);
		}
	}

	private void installDefaultFlows(String nodeUri, InstanceIdentifier<FlowCapableNode> nodePath) {
		if (idsProvider.isNpeSwitch(nodeUri)) {
			this.installSendIdsHelloToControllerFlow(nodeUri, nodePath);
			// Set the MPLS label to VPN label matches.
			// TODO
		}
		PacketInDispatcher packetInDispatcher = new PacketInDispatcher(nodeUri, nodePath, idsProvider);
		idsProvider.getNotificationService().registerNotificationListener(packetInDispatcher);
		ListenerRegistration<PacketInDispatcher> registration = idsProvider.getNotificationService()
				.registerNotificationListener(packetInDispatcher);
		packetInDispatcher.setListenerRegistration(registration);
	}

	public synchronized void installDefaultFlows() {
		for (InstanceIdentifier<FlowCapableNode> node : this.pendingNodes) {
			String nodeUri = InstanceIdentifierUtils.getNodeUri(node);
			this.installDefaultFlows(nodeUri, node);
		}
		this.pendingNodes.clear();
	}

	/**
	 * This gets invoked when a switch appears and connects.
	 * 
	 * @param nodePath
	 *            -- the node path.
	 *
	 */
	public synchronized void onFlowCapableSwitchAppeared(InstanceIdentifier<FlowCapableNode> nodePath) {

		String nodeUri = nodePath.firstKeyOf(Node.class).getId().getValue();

		LOG.info("onFlowCapableSwitchAppeared");
		// The URI identifies the node instance.
		LOG.info("node URI " + nodeUri + " nodePath " + nodePath);
		// Stash away the URI to node path so we can reference it later.
		this.idsProvider.putInUriToNodeMap(nodeUri, nodePath);

		if (idsProvider.getTopology() != null) {
			this.installDefaultFlows(nodeUri, nodePath);
		} else {
			this.pendingNodes.add(nodePath);
		}

	}

	/**
	 * Deal with disconnection of the switch.
	 * 
	 * @param nodePath
	 *            - the instance id of the disconnecting switch.
	 */

	public synchronized void onFlowCapableSwitchDisappeared(InstanceIdentifier<FlowCapableNode> nodePath) {
		String nodeUri = nodePath.firstKeyOf(Node.class).getId().getValue();
		LOG.info("onFlowCapableSwitchDisappeared");
		// The URI identifies the node instance.
		LOG.info("node URI " + nodeUri);
		// Remove the node URI from the uriToNodeMap.
		// Remove the node URI from our switches table.
	}

}
