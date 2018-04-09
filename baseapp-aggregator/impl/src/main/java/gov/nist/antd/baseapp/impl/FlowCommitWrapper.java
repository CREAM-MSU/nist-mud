/*
 *Copyright (c) Public Domain
 * 
 *This program and the accompanying materials are made available under the Public Domain.
 * 
 * This file includes code developed by employees of the National Institute of
 * Standards and Technology (NIST)
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), and others. This software has been
 * contributed to the public domain. Pursuant to title 15 Untied States
 * Code Section 105, works of NIST employees are not subject to copyright
 * protection in the United States and are considered to be in the public
 * domain. As a result, a formal license is not needed to use this software.
 *
 * This software is provided "AS IS." NIST MAKES NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * NON-INFRINGEMENT AND DATA ACCURACY. NIST does not warrant or make any
 * representations regarding the use of the software or the results thereof,
 * including but not limited to the correctness, accuracy, reliability or
 * usefulness of this software.
 */
package gov.nist.antd.baseapp.impl;

import java.util.concurrent.Semaphore;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Instruction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;

/**
 * Flow commit wrapper.
 */

class FlowCommitWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(FlowCommitWrapper.class);

	private DataBroker dataBrokerService;

	FlowCommitWrapper(DataBroker dataBrokerService) {
		this.dataBrokerService = dataBrokerService;
	}

	synchronized void writeFlow(FlowBuilder flow, InstanceIdentifier<FlowCapableNode> flowNodeIdent) {
		writeFlow(flow.build(), flowNodeIdent);
	}

	private synchronized void writeFlow(Flow flow, InstanceIdentifier<FlowCapableNode> flowNodeIdent) {
		ReadWriteTransaction modification = dataBrokerService.newReadWriteTransaction();
		LOG.info("writeFlow : " + flowNodeIdent + " Flow : " + flow.getFlowName() + " tableId " + flow.getTableId()
				+ " flowId " + flow.getId().getValue());

		LOG.info(flow.toString());
		for (Instruction instruction : flow.getInstructions().getInstruction()) {
			LOG.info("writeFlow: Instruction =  " + instruction.getInstruction().toString());
		}

		final InstanceIdentifier<Flow> path1 = flowNodeIdent.child(Table.class, new TableKey(flow.getTableId()))
				.child(Flow.class, flow.getKey());

		modification.merge(LogicalDatastoreType.CONFIGURATION, path1, flow, true);
		modification.submit();
	}


}
