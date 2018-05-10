package environment;

import models.VertexSettings;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.List;

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
        shortestPaths.getShortestPathsCount(); // force calculating
        return this;
    }

    public double getPathWeight(String source, String to)
    {
        return shortestPaths.getPathWeight(source, to);
    }

    public List<String> getShortestPath(String source, String to) {
        return shortestPaths.getPath(source, to).getVertexList();
    }

    public List<String> getShortestPath(List<String> requiredVertexes) {
        if (requiredVertexes.size() == 0)
            return new ArrayList<>();
        if (requiredVertexes.size() == 1)
            return new ArrayList<>(requiredVertexes);

        var shortestPath = new ArrayList<String>();
        for (int i = 0; i < requiredVertexes.size() - 1; i++){
            var path = getShortestPath(requiredVertexes.get(i), requiredVertexes.get(i + 1));
            shortestPath.addAll(path.subList(0, path.size() - 1));
        }
        shortestPath.add(requiredVertexes.get(requiredVertexes.size() - 1));
        return shortestPath;
    }

}
