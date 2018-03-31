package environment;

import models.VertexSettings;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;

/**
 * Created by K750JB on 24.03.2018.
 */
public class Map {
    private static Map ourInstance = new Map();

    public SimpleWeightedGraph<String, DefaultWeightedEdge> Graph =
            new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

    public static Map GetInstance() {
        return ourInstance;
    }

    private Map() {
    }

    public Map Initialize(ArrayList<VertexSettings> vertices) {
        for (VertexSettings vertex : vertices)
        {
            Graph.addVertex(vertex.Name);
        }
        for (VertexSettings startVertex : vertices)
        {
            for (String endVertexName : startVertex.AdjacentVertices)
            {
                Graph.setEdgeWeight(Graph.addEdge(startVertex.Name, endVertexName), 1);
            }
        }
        return this;
    }
}
