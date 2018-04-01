package environment;

import models.VertexSettings;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;

/**
 * Created by K750JB on 24.03.2018.
 */
public class Map {
    private static Map ourInstance = new Map();

    private FloydWarshallShortestPaths<String, DefaultWeightedEdge> shortestPaths;

    public SimpleWeightedGraph<String, DefaultWeightedEdge> Graph =
            new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

    public static Map GetInstance() {
        return ourInstance;
    }
    private Map() {}

    public Map Initialize(ArrayList<VertexSettings> vertices) {
        for (VertexSettings vertex : vertices)
        {
            Graph.addVertex(vertex.Name);
        }
        for (VertexSettings startVertex : vertices)
        {
            for (String endVertexName : startVertex.AdjacentVertices)
            {
                var edge = Graph.addEdge(startVertex.Name, endVertexName);
                if (edge != null) // could be added before
                {
                    Graph.setEdgeWeight(edge, 1);
                }
            }
        }
        shortestPaths = new FloydWarshallShortestPaths<>(Graph);

        return this;
    }

    public int GetPathWeight(String source, String to)
    {
        return (int) shortestPaths.getPathWeight(source, to);
    }
}
