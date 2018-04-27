/*
 *  Copyright (c) None.
 *
 * Works of NIST employees are released to the public domain according to the following:
 *
 * This software was developed by employees of the National Institute of Standards and Technology
 * (NIST), and others. This software has been contributed to the public domain. Pursuant to title 15
 * Untied States Code Section 105, works of NIST employees are not subject to copyright protection
 * in the United States and are considered to be in the public domain. As a result, a formal license
 * is not needed to use this software.
 *
 * This software is provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED OR
 * STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY. NIST does not warrant or make any
 * representations regarding the use of the software or the results thereof, including but not
 * limited to the correctness, accuracy, reliability or usefulness of this software.
 */

package gov.nist.antd.vlan.impl;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InstanceIdentifierUtils {
    private static AtomicLong flowIdInc = new AtomicLong();

    private static final Logger LOG = LoggerFactory
            .getLogger(InstanceIdentifierUtils.class);

    private InstanceIdentifierUtils() {
        // hiding constructor for util class
    }

    static FlowId createFlowId(String uri) {
        return new FlowId(
                uri + "/vlan/" + String.valueOf(flowIdInc.getAndIncrement()));
    }

    static FlowCookie createFlowCookie(String flowCookieId) {
        return new FlowCookie(
                BigInteger.valueOf(Math.abs(getFlowHash(flowCookieId))));
    }

    private static int getFlowHash(String flowSpec) {
        // Has to be within the size of an mpls label.
        // or the flow does not appear.
        return Math.abs(flowSpec.hashCode()) % (1 << 20);
    }

    public static final String getNodeUri(
            InstanceIdentifier<FlowCapableNode> node) {
        return node.firstKeyOf(Node.class).getId().getValue();

    }

}
