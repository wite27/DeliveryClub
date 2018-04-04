package environment;

import models.VertexSettings;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;

/**
 * Created by K750JB on 24.03.2018.
 */
public class CityMap {
    private static CityMap ourInstance = new CityMap();

    private FloydWarshallShortestPaths<String, DefaultWeightedEdge> shortestPaths;

    public SimpleWeightedGraph<String, DefaultWeightedEdge> Graph =
            new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

    public static CityMap getInstance() {
        return ourInstance;
    }
    private CityMap() {}

    public CityMap Initialize(ArrayList<VertexSettings> vertices) {
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

    public int getPathWeight(String source, String to)
    {
        return (int) shortestPaths.getPathWeight(source, to);
    }
}
