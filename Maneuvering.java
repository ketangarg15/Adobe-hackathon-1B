public class Maneuvering {
    public static int numberofPaths(int m,int n){
        if(m==1 || n==1){
            return 1;
        }
        return numberofPaths(m-1, n)+numberofPaths(m, n-1);
    }
    public static void main(String[] args){
        System.out.println(numberofPaths(3,3));
    }
}
