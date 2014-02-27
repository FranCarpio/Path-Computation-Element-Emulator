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

import java.util.concurrent.LinkedBlockingQueue;
import com.graph.graphcontroller.Gcontroller;
import com.pcee.architecture.ModuleManagement;
import com.pcee.architecture.computationmodule.ted.TopologyInformation;
import com.pcee.logger.Logger;
import com.pcee.protocol.message.PCEPMessage;

/**
 * ThreadPool implementation to support multiple path computations, also
 * includes an update server to support topology updates
 * 
 * @author Mohit Chamania
 * @author Marek Drogon
 * @author Fran Carpio
 */
public class GurobiThreadPool {

	// boolean to check if the the thread pool has been initialized
	private boolean isInitialized;

	// Blocking queue used by workers to read incoming requests
	private LinkedBlockingQueue<PCEPMessage> requestQueue;

	// Graph instance used by workers to perform path computations
	private Gcontroller graph;

	// Module management instance to send response to the computation layer
	private ModuleManagement lm;

	// TopologyInformation used to retrieve topology information from file
	private static TopologyInformation topologyInstance = TopologyInformation
			.getInstance(true);

	// Thread instance
	GurobiWorker thread;

	// Integer to set up the maximum number of request in the buffer before the
	// Gurobi optimization.
	private int numberOfRequests;

	/**
	 * default Constructor
	 * 
	 * @param layerManagement
	 * @param requestQueue
	 * @param numberOfRequests
	 */
	public GurobiThreadPool(ModuleManagement layerManagement,
			LinkedBlockingQueue<PCEPMessage> requestQueue, int numberOfRequests) {
		lm = layerManagement;
		isInitialized = false;
		this.requestQueue = requestQueue;
		graph = topologyInstance.getGraph();
		this.numberOfRequests = numberOfRequests;
		initThread();
	}

	/**
	 * Function to initialize the thread
	 * 
	 * @return
	 */
	private boolean initThread() {
		localLogger("Initializing GUROBI thread");
		if (isInitialized == false) {
			thread = new GurobiWorker(lm, requestQueue, graph, numberOfRequests);
			thread.start();
			localLogger("Worker Thread initialized");
			isInitialized = true;
			return true;
		} else
			return false;
	}

	/**
	 * Function to get the new graph instance
	 * 
	 * @return
	 */
	protected Gcontroller getUpdatedGraph() {
		return topologyInstance.getGraph();
	}

	/** Function to stop the thread */
	public void stop() {
		thread.setTerminate(true);
		thread.interrupt();
	}

	/**
	 * Function to log events inside the thread implementation
	 * 
	 * @param event
	 */
	private void localLogger(String event) {
		Logger.logSystemEvents("[Thread]     " + event);
	}

}
