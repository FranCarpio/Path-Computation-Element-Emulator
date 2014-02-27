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
package com.pcee.architecture.computationmodule.gurobi.impl;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.ArrayList;
import java.util.List;

import com.graph.elements.edge.EdgeElement;
import com.graph.elements.vertex.VertexElement;
import com.graph.graphcontroller.Gcontroller;
import com.graph.path.PathElement;
import com.graph.path.algorithms.constraints.Constraint;

/**
 * GUROBI model
 * 
 * @author Fran Carpio
 */
public class GurobiModel {

	List<Boolean> routing = new ArrayList<Boolean>();
	List<PathElement> setOfPaths = new ArrayList<PathElement>();
	List<VertexElement> setOfNodes = new ArrayList<VertexElement>();
	List<EdgeElement> setOfLinks = new ArrayList<EdgeElement>();

	public void start(Gcontroller graph, List<Constraint> constraints) {

		try {
			GRBEnv env = new GRBEnv();
			GRBModel model = new GRBModel(env);
			GRBLinExpr expr = new GRBLinExpr();

			/** Parameters */
			routing = new ArrayList<Boolean>();
			setOfNodes.addAll(graph.getVertexSet());
			setOfLinks.addAll(graph.getEdgeSet());

			PathsComputation pathsComputation = new PathsComputation();
			for (Constraint c : constraints)
				setOfPaths.addAll(pathsComputation.allPathsFromStoD(graph, 2,
						c.getSource(), c.getDestination()));

			/** Variables */

			/**
			 * R Boolean, Routing Variable, true if path p is used
			 */
			GRBVar[] R = new GRBVar[setOfPaths.size()];
			for (int p = 0; p < setOfPaths.size(); ++p) {
				R[p] = model.addVar(0.0, 1.0, setOfPaths.get(p).getPathParams()
						.getPathDelay(), GRB.BINARY, "R[" + p + "]");
			}

			/**
			 * Gamma Double, Transit at link L
			 */
			GRBVar[] G = new GRBVar[setOfLinks.size()];
			for (int l = 0; l < setOfLinks.size(); ++l) {
				G[l] = model.addVar(0.0, setOfLinks.get(l).getEdgeParams()
						.getAvailableCapacity(), 0.0, GRB.CONTINUOUS, "G[" + l
						+ "]");
			}

			model.update();

			/**
			 * Constraints
			 */

			/**
			 * (2) sum_p Rp*travp(l) * lambdaSD * conn_p (s,d) = G[l]
			 */
			for (int l = 0; l < setOfLinks.size(); ++l) {
				expr = new GRBLinExpr();
				for (int s = 0; s < setOfNodes.size(); s++) {
					VertexElement source = setOfNodes.get(s);
					for (int d = 0; d < setOfNodes.size(); d++) {
						VertexElement destination = setOfNodes.get(d);
						if (source.equals(destination))
							continue;
						for (int p = 0; p < setOfPaths.size(); ++p) {
							if (!setOfPaths.get(p).isLinktraversed(
									setOfLinks.get(l)))
								continue;
							if (!setOfPaths.get(p).isConnected(source,
									destination))
								continue;

							double traffic = 0.0;
							for (Constraint c : constraints) {
								if (c.getSource().equals(source)
										&& c.getDestination().equals(
												destination))
									traffic = c.getBw();
							}
							expr.addTerm(traffic, R[p]);
						}
					}
				}

				model.addConstr(expr, GRB.EQUAL, G[l], "Constraint 2");
			}

			/**
			 * (4.1) sum_p [Rp * connp(s,d)] <= 1
			 */
			for (int s = 0; s < setOfNodes.size(); s++) {
				VertexElement source = setOfNodes.get(s);
				for (int d = 0; d < setOfNodes.size(); d++) {
					VertexElement destination = setOfNodes.get(d);
					if (setOfNodes.get(s).equals(setOfNodes.get(d)))
						continue;
					expr = new GRBLinExpr();
					for (int p = 0; p < setOfPaths.size(); p++) {
						if (setOfPaths.get(p).isConnected(source, destination)) {
							expr.addTerm(1.0, R[p]);
						}
					}
					model.addConstr(expr, GRB.LESS_EQUAL, 1, "Constraint 4.1");
				}
			}

			/**
			 * (4.2) lamda_sd/Max <= sum_p [Rp * connp(s,d)]
			 */

			for (Constraint c : constraints) {
				expr = new GRBLinExpr();
				for (int p = 0; p < setOfPaths.size(); ++p) {
					if (!setOfPaths.get(p).isConnected(c.getSource(),
							c.getDestination()))
						continue;
					expr.addTerm(1.0, R[p]);
				}
				double constant = c.getBw() * 0.01;
				model.addConstr(expr, GRB.GREATER_EQUAL, constant,
						"Constraint 4.2");
			}

			/**
			 * (5) Rp * conn_p(s,d) * TAU_p <= TAU_s,d
			 */

			for (Constraint c : constraints) {
				expr = new GRBLinExpr();
				for (int p = 0; p < setOfPaths.size(); ++p) {
					if (!setOfPaths.get(p).isConnected(c.getSource(),
							c.getDestination()))
						continue;
					expr.addTerm(setOfPaths.get(p).getPathParams()
							.getPathDelay(), R[p]);
				}
				model.addConstr(expr, GRB.LESS_EQUAL, c.getMaxDelay(),
						"Constraint 5");
			}

			model.optimize();

			for (int p = 0; p < setOfPaths.size(); p++) {
				if (R[p].get(GRB.DoubleAttr.X) == 1.0)
					routing.add(true);
				else
					routing.add(false);
			}

			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". "
					+ e.getMessage());
		}
	}

	public PathElement getOptimalPath(Constraint constraint) {

		PathElement pathElement = null;

		for (int b = 0; b < routing.size(); b++) {
			if (routing.get(b)) {
				if (setOfPaths.get(b).getSource().getVertexID()
						.equals(constraint.getSource().getVertexID())
						&& setOfPaths
								.get(b)
								.getDestination()
								.getVertexID()
								.equals(constraint.getDestination()
										.getVertexID())) {
					pathElement = setOfPaths.get(b);
				}
			}
		}
		return pathElement;
	}
}
