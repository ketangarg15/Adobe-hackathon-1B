import java.util.*;
public class HamiltonianCycle {
    static List<List<Integer>> allpaths=new ArrayList<>();
    static int V;
    public static void findallhamiltonianCycle(int graph[][]){
        V=graph.length;
        boolean[] visited=new boolean[V];
        List<Integer> path=new ArrayList<>();
        path.add(0);
        visited[0]=true;
        dfs(graph,0,visited,path);
        if(allpaths.size()==0){
            System.out.println("not found");
        }
        else {
            for (List<Integer> p : allpaths) {
                System.out.println(p);
            }
        }
    }
    public static void dfs(int graph[][],int node,boolean visited[],List<Integer> path){
        if(path.size()==V){
            if(graph[node][0]==1){
                List<Integer> cycle=new ArrayList<>(path);
                cycle.add(0);
                allpaths.add(cycle);
            }
            return;
        }
        for(int next=0;next<V;next++){
            if(graph[node][next]==1&& !visited[next]){
                visited[next]=true;
                path.add(next);
                dfs(graph,next,visited,path);
                visited[next]=false;
                path.remove(path.size()-1);
            }
        }
    }
     public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int V = sc.nextInt();
        int[][] graph = new int[V][V];

        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                graph[i][j] = sc.nextInt();
            }
        }

        findallhamiltonianCycle(graph);
    }
}