import java.util.*;
public class NaturalSort {
    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        List<String> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            items.add(sc.next());
        }
        items.sort((x, y) -> {
            String px = x.replaceAll("\\d", "");
            String py = y.replaceAll("\\d", "");
            if (!px.equals(py)) {
                return px.compareTo(py);
            }
            int nx = Integer.parseInt(x.replaceAll("\\D", ""));
            int ny = Integer.parseInt(y.replaceAll("\\D", ""));
            return Integer.compare(nx, ny);
        });

        items.sort((x,y)->{
            String px=x.replaceAll("\\d","");
            String py=y.replaceAll("\\d","");
            if(!px.equals(py)){
                return px.compareTo(py);
            }
            int nx=Integer.parseInt(x.replaceAll("\\D",""));
            int ny=Integer.parseInt(x.replaceAll("\\D", ""));
            return Integer.compare(nx, ny);
        });
        for (String st : items) {
            System.out.println(st);
        }
    }
}
