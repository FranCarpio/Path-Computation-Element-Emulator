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
import com.pcee.protocol.message.PCEPMessage;

public class GurobiWorker extends Thread {

	private LinkedBlockingQueue<PCEPMessage> requestQueue;
	private ModuleManagement lm;
	private boolean terminateWorker = false;
	private int numberOfRequests;

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
			LinkedBlockingQueue<PCEPMessage> requestQueue, Gcontroller graph,
			int numberOfRequests) {
		lm = layerManagement;
		this.requestQueue = requestQueue;
		this.numberOfRequests = numberOfRequests;
	}

	public void run() {
		while (!terminateWorker) {

			GurobiTask task = new GurobiTask(lm, requestQueue,
					TopologyInformation.getInstance(true).getGraph()
							.createCopy(), numberOfRequests);
			task.run();

			if (Thread.currentThread().isInterrupted()) {
				if (terminateWorker) {
					break;
				}
				continue;
			}
		}
	}
}
