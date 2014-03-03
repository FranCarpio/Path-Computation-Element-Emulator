/**
 *  This file is part of Path Computation Element Emulator (PCEE).
 *
 *  PCEE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PCEE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PCEE.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.pcee.architecture.computationmodule.gurobi;

import java.util.ArrayList;
import java.util.List;

import com.graph.elements.vertex.VertexElement;
import com.graph.graphcontroller.Gcontroller;
import com.graph.path.PathElement;
import com.graph.path.algorithms.constraints.Constraint;
import com.graph.path.algorithms.constraints.impl.SimplePathComputationConstraint;
import com.pcee.architecture.ModuleEnum;
import com.pcee.architecture.ModuleManagement;
import com.pcee.architecture.computationmodule.gurobi.impl.GurobiModel;
import com.pcee.architecture.computationmodule.gurobi.impl.PathsComputation;
import com.pcee.logger.Logger;
import com.pcee.protocol.message.PCEPMessage;
import com.pcee.protocol.message.PCEPMessageFactory;
import com.pcee.protocol.message.objectframe.PCEPObjectFrameFactory;
import com.pcee.protocol.message.objectframe.impl.PCEPBandwidthObject;
import com.pcee.protocol.message.objectframe.impl.PCEPExplicitRouteObject;
import com.pcee.protocol.message.objectframe.impl.PCEPNoPathObject;
import com.pcee.protocol.message.objectframe.impl.PCEPRequestParametersObject;
import com.pcee.protocol.message.objectframe.impl.erosubobjects.EROSubobjects;
import com.pcee.protocol.message.objectframe.impl.erosubobjects.PCEPAddress;
import com.pcee.protocol.request.PCEPRequestFrame;
import com.pcee.protocol.request.PCEPRequestFrameFactory;
import com.pcee.protocol.response.PCEPResponseFrame;
import com.pcee.protocol.response.PCEPResponseFrameFactory;

/**
 * Runnable class used by the thread pool to process path computation requests
 * 
 * @author Mohit Chamania
 * @author Marek Drogon
 */
public class GurobiWorkerTask implements Runnable {
	// Request to be processed
	private List<PCEPMessage> requestList;
	// Graph used for computation of the request
	private Gcontroller graph;
	// Module management object to send the response to the session layer
	private ModuleManagement lm;

	PathsComputation pathsComputation;

	List<Constraint> constraintsList;

	/** Default Constructor */
	public GurobiWorkerTask(ModuleManagement layerManagement,
			List<PCEPMessage> requestList, Gcontroller graph,
			PathsComputation pathsComputation) {
		lm = layerManagement;
		this.requestList = requestList;
		this.graph = graph;
		this.pathsComputation = pathsComputation;
	}

	/** Function to update the graph instance used for computation */
	public void updateGraph(Gcontroller newGraph) {
		this.graph = newGraph;
	}

	/** Function to implement the path computation operations */
	public void run() {
		List<PCEPRequestFrame> requestFrameList = new ArrayList<PCEPRequestFrame>();
		constraintsList = new ArrayList<Constraint>();

		for (int i = 0; i < requestList.size(); i++) {
			PCEPMessage request = requestList.get(i);
			PCEPRequestFrame requestFrame = PCEPRequestFrameFactory
					.getPathComputationRequestFrame(request);
			requestFrameList.add(requestFrame);
			localLogger("Starting Processing of Request: "
					+ requestFrame.getRequestID());
			setConstraint(requestFrame, request);
			localLogger("Completed Processing of Request: "
					+ requestFrame.getRequestID());
		}

		GurobiModel gurobiModel = new GurobiModel();
		gurobiModel.start(graph, constraintsList, pathsComputation);

		int count = 0;
		for (PCEPRequestFrame request : requestFrameList) {
			generatePCEPResponseMessage(
					gurobiModel.getOptimalPath(constraintsList.get(count)),
					request, requestList.get(count));
			count++;
		}
	}

	private void setConstraint(PCEPRequestFrame requestFrame,
			PCEPMessage request) {
		String sourceID = requestFrame.getSourceAddress().getIPv4Address(false)
				.trim();
		String destID = requestFrame.getDestinationAddress()
				.getIPv4Address(false).trim();
		if (graph.vertexExists(sourceID) && graph.vertexExists(destID)) {
			Constraint constr = null;
			if (requestFrame.containsBandwidthObject()
					&& !requestFrame.containsMetricObjectList()) {
				localLogger("Request Contains bandwidth Object");
				constr = new SimplePathComputationConstraint(
						graph.getVertex(sourceID), graph.getVertex(destID),
						requestFrame.extractBandwidthObject()
								.getBandwidthFloatValue());
			}
			if (requestFrame.containsBandwidthObject()
					&& requestFrame.containsMetricObjectList()) {
				localLogger("Request Contains bandwidth and delay Object");
				constr = new SimplePathComputationConstraint(
						graph.getVertex(sourceID), graph.getVertex(destID),
						requestFrame.extractBandwidthObject()
								.getBandwidthFloatValue(), requestFrame
								.extractMetricObjectList().getFirst()
								.getDelayFloatValue());
			} else {
				constr = new SimplePathComputationConstraint(
						graph.getVertex(sourceID), graph.getVertex(destID));
			}

			constraintsList.add(constr);
		} else {
			// Source and/or destination not present in the PCE
			if (graph.vertexExists(sourceID))
				localLogger("Destination IP address " + destID
						+ " not in the topology. Returning a no path object");
			else if (graph.vertexExists(destID))
				localLogger("Source IP address " + sourceID
						+ " not in the topology. Returning a no path object");
			else {
				localLogger("Both source IP address " + sourceID
						+ " and destination IP address " + destID
						+ " not in the topology. Returning a no path object");
			}
			returnNoPathMessage(requestFrame.getRequestID(), request);
		}

	}

	private void generatePCEPResponseMessage(PathElement element,
			PCEPRequestFrame requestFrame, PCEPMessage request) {

		if (element != null) {
			localLogger("Computed path is " + element.getVertexSequence());
			// return response
			ArrayList<EROSubobjects> vertexList = getTraversedVertexes(element
					.getTraversedVertices());

			// Generate ERO Object
			PCEPExplicitRouteObject ERO = PCEPObjectFrameFactory
					.generatePCEPExplicitRouteObject("1", "0", vertexList);
			// atleast one path was computed
			PCEPRequestParametersObject RP = PCEPObjectFrameFactory
					.generatePCEPRequestParametersObject("1", "0", "0", "0",
							"0", "1",
							Integer.toString(requestFrame.getRequestID()));

			PCEPResponseFrame respFrame = PCEPResponseFrameFactory
					.generatePathComputationResponseFrame(RP);

			respFrame.insertExplicitRouteObject(ERO);

			if (requestFrame.containsBandwidthObject()) {
				PCEPBandwidthObject bw = PCEPObjectFrameFactory
						.generatePCEPBandwidthObject("1", "0", (float) element
								.getPathParams().getAvailableCapacity());
				respFrame.insertBandwidthObject(bw);
			}

			PCEPMessage mesg = PCEPMessageFactory.generateMessage(respFrame);
			mesg.setAddress(request.getAddress());

			localLogger("Path found in the domain. Sending back to client");
			// Send response message from the computation layer to the
			// session layer
			lm.getComputationModule().sendMessage(mesg,
					ModuleEnum.SESSION_MODULE);

		} else {
			// No path Found in the source domain return no path Object
			returnNoPathMessage(requestFrame.getRequestID(), request);
		}

	}

	/** Function to return the no Path message to the Client */
	protected void returnNoPathMessage(int requestID, PCEPMessage request) {
		// Generate a No path object
		PCEPRequestParametersObject RP = PCEPObjectFrameFactory
				.generatePCEPRequestParametersObject("1", "0", "0", "0", "0",
						"1", Integer.toString(requestID));
		PCEPNoPathObject noPath = PCEPObjectFrameFactory
				.generatePCEPNoPathObject("1", "0", 1, "0");
		PCEPResponseFrame responseFrame = PCEPResponseFrameFactory
				.generatePathComputationResponseFrame(RP);
		responseFrame.insertNoPathObject(noPath);
		PCEPMessage mesg = PCEPMessageFactory.generateMessage(responseFrame);
		mesg.setAddress(request.getAddress());
		lm.getComputationModule().sendMessage(mesg, ModuleEnum.SESSION_MODULE);
	}

	/**
	 * Function to get the list of traversed vertices as PCEP addresses from the
	 * List of vertices in the graph. Used to create ERO
	 * 
	 * @param resp
	 * @return
	 */
	protected ArrayList<EROSubobjects> getTraversedVertexes(
			ArrayList<VertexElement> vertexArrayList) {

		ArrayList<EROSubobjects> traversedVertexesList = new ArrayList<EROSubobjects>();

		for (int i = 0; i < vertexArrayList.size(); i++) {
			traversedVertexesList.add(new PCEPAddress(vertexArrayList.get(i)
					.getVertexID(), false));
		}
		return traversedVertexesList;
	}

	/**
	 * Function to log events inside the WorkerTask
	 * 
	 * @param event
	 */
	private void localLogger(String event) {
		Logger.logSystemEvents("[WorkerTask]     " + event);
	}
}
