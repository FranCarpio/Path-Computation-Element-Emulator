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

package com.graph.path.pathelementimpl;

import java.util.ArrayList;
import java.util.Stack;

import com.graph.elements.edge.EdgeElement;
import com.graph.elements.vertex.VertexElement;
import com.graph.graphcontroller.Gcontroller;
import com.graph.logger.GraphLogger;
import com.graph.path.PathElement;
import com.graph.path.params.PathParams;

public class PathElementImpl extends PathElement {

	private static final String classIdentifier = "PathElementImpl";

	private ArrayList<VertexElement> vertices;
	private ArrayList<EdgeElement> edges;

	public void setVertices(ArrayList<VertexElement> vertices) {
		this.vertices = vertices;
	}

	public PathElementImpl(Gcontroller graph, VertexElement source,
			VertexElement destination) {
		this.graph = graph;
		this.source = source;
		this.destination = destination;
		edges = new ArrayList<EdgeElement>();
		this.setPathParams(new PathParams(this));
	}

	public PathElementImpl(Gcontroller graph, VertexElement source,
			VertexElement destination, ArrayList<EdgeElement> initEdges) {
		this.graph = graph;
		this.source = source;
		this.destination = destination;
		edges = new ArrayList<EdgeElement>();
		for (int i = 0; i < initEdges.size(); i++) {
			edges.add(initEdges.get(i));
		}
		this.setPathParams(new PathParams(this));
		this.updateVertexSequence();
	}

	public void insertEdge(EdgeElement edge) {
		if (edges == null) {
			edges = new ArrayList<EdgeElement>();
		}
		edges.add(edge);
		this.updateVertexSequence();
	}

	/** Function to create the vertexsequence from the edge list */
	private void updateVertexSequence() {
		if (vertices == null) {
			vertices = new ArrayList<VertexElement>();
		}
		vertices.clear();
		vertices.add(this.getSource());
		VertexElement currentVertex = this.getSource();
		if (edges == null) {
			GraphLogger.logError(
					"No Edges in EdgeList to create vertex sequence",
					classIdentifier);
		} else {
			for (int i = 0; i < edges.size(); i++) {
				if (edges.get(i).getSourceVertex().compareTo(currentVertex) == 0) {
					currentVertex = edges.get(i).getDestinationVertex();
				} else {
					currentVertex = edges.get(i).getSourceVertex();
				}
				vertices.add(currentVertex);
			}
		}
	}

	public boolean containsEdge(EdgeElement edge) {
		return edges.contains(edge);
	}

	public boolean containsVertex(VertexElement vertex) {
		return vertices.contains(vertex);
	}

	public ArrayList<EdgeElement> getTraversedEdges() {
		return edges;
	}

	public ArrayList<VertexElement> getTraversedVertices() {
		return vertices;
	}

	/** Function to reserve Bandwidth on a path */
	public boolean resvBandwidth(double bw) {
		ArrayList<EdgeElement> list = this.getTraversedEdges();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getEdgeParams().reserveCapacity(bw) == false) {
				{
					GraphLogger.logError("Error during capacity reservation",
							classIdentifier);
					for (int j = 0; j < i; j++) {
						list.get(j).getEdgeParams().releaseCapacity(bw);
					}
					return false;
				}
			}
		}
		return true;
	}

	/** Function to release Bandwidth on a path */
	public boolean releaseBandwidth(double bw) {
		ArrayList<EdgeElement> list = this.getTraversedEdges();
		int flag = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getEdgeParams().releaseCapacity(bw) == false) {
				{
					GraphLogger.logError("Error during releasing capacity",
							classIdentifier);
					flag = 1;
				}
			}
		}
		if (flag == 0)
			return true;
		else
			return false;
	}

	/** Function to set the edge Sequence of a path Element */
	public void setEdgeSequence(ArrayList<EdgeElement> edges) {
		this.edges = edges;
		this.updateVertexSequence();
	}

	/** Function to know if the link (edge) is traversed by the path */
	public boolean isLinktraversed(EdgeElement edge) {

		boolean traversed = false;

		for (EdgeElement e : edges) {
			if (e.getEdgeID().equals(edge.getEdgeID()))
				traversed = true;
		}

		return traversed;
	}

	/**
	 * Function to know if the path is connected by the source node "s" and
	 * destination node "d"
	 */
	public boolean isConnected(VertexElement s, VertexElement d) {
		boolean isConnected = false;

		if (s.getVertexID().equals(source.getVertexID())
				&& d.getVertexID().equals(destination.getVertexID()))
			isConnected = true;

		return isConnected;
	}

	/** Function to set the edges of a path element, from a stack */
	public void setEdges(Stack<VertexElement> pathNodes) {

		int i = 0;
		boolean allLinks = false;
		for (VertexElement n : pathNodes) {
			if (!allLinks) {
				for (EdgeElement l : n.getConnectedEdges()) {
					if (l.getDestinationVertex().equals(pathNodes.get(i + 1))) {
						edges.add(l);
						if (edges.size() + 1 == pathNodes.size()) {
							allLinks = true;
						}
					}
				}
				i++;
			}
		}
	}
}
