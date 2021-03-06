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
import java.util.concurrent.LinkedBlockingQueue;

import com.graph.graphcontroller.Gcontroller;
import com.pcee.architecture.ModuleManagement;
import com.pcee.architecture.computationmodule.gurobi.impl.PathsComputation;
import com.pcee.architecture.computationmodule.ted.TopologyInformation;
import com.pcee.logger.Logger;
import com.pcee.protocol.message.PCEPMessage;

public class GurobiWorker extends Thread {

	private String ID;
	private LinkedBlockingQueue<PCEPMessage> requestQueue;
	private ModuleManagement lm;
	private boolean terminateWorker = false;
	PathsComputation pathsComputation;
	int numberOfRequest;

	/**
	 * Function to set the flag to terminate the worker thread
	 * 
	 * @param value
	 */
	public void setTerminate(boolean value) {
		this.terminateWorker = value;
	}

	/**
	 * Default Constructor
	 * 
	 * @param layerManagement
	 * @param pool
	 * @param ID
	 * @param requestQueue
	 * @param graph
	 */
	public GurobiWorker(ModuleManagement layerManagement,
			GurobiThreadPool pool, String ID,
			LinkedBlockingQueue<PCEPMessage> requestQueue, Gcontroller graph,
			int numberOfRequest, PathsComputation pathsComputation) {
		lm = layerManagement;
		this.ID = ID;
		this.requestQueue = requestQueue;
		this.pathsComputation = pathsComputation;
		this.numberOfRequest = numberOfRequest;
	}

	/** Main run method of the worker thread */
	public void run() {

		localLogger("Initializing Worker Thread ID = " + ID);
		List<PCEPMessage> requestList;
		int flag = 0;
		while (!terminateWorker) {
			GurobiWorkerTask task = null;
			requestList = new ArrayList<PCEPMessage>();
			try {
				if (flag == 0) {
					for (int i = 0; i < numberOfRequest; i++) {
						requestList.add(requestQueue.take());
						// Record the leaving Queue Time for each request
						// localLogger("Starting request ID " +
						// request.getRequestID());
						localLogger("Current Length of Request Queue = "
								+ requestQueue.size());
					}
				}
			} catch (InterruptedException e) {
				if (terminateWorker) {
					localDebugger("Stopping Worker Thread : " + ID);
					break;
				}
				continue;
			}
			// Flag to check if thread was interrupted during a wait operation
			// or during a computation
			flag = 1;
			if (requestList != null) {
				task = new GurobiWorkerTask(lm, requestList,
						TopologyInformation.getInstance(false).getGraph()
								.createCopy(), pathsComputation);
				task.run();
				// localLogger("Completed processing of request ID " +
				// request.getRequestID());
			}
			if (Thread.currentThread().isInterrupted()) {
				if (terminateWorker) {
					localDebugger("Stopping Worker Thread : " + ID);
					break;
				}
				continue;
			}
			// The request was computed successfully, and the flag variable is
			// set to indicate
			// that a new request be processed in the next iteration
			flag = 0;
		}
	}

	/**
	 * Function to log events in the worker thread
	 * 
	 * @param event
	 */
	private void localLogger(String event) {
		Logger.logSystemEvents("[Worker " + ID + "]     " + event);
	}

	/**
	 * Function to log debugging events
	 * 
	 * @param event
	 */
	private void localDebugger(String event) {
		Logger.debugger("[Worker " + ID + "]     " + event);
	}
}
