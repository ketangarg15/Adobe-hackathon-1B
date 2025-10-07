public class Hourglass {
    public static int maxhourglasssum(int mat[][]){
        int row=mat.length;
        int col=mat[0].length;
        int max=Integer.MIN_VALUE;
        for(int i=0;i<row-2;i++){
            for(int j=0;j<col-2;j++){
                int sum=mat[i][j]+mat[i][j+1]+mat[i][j+2]+mat[i+1][j+1]+mat[i+2][j]+mat[i+2][j+1]+mat[i+2][j+2];
                max=Math.max(sum,max);
            }
        }
        return max;
    }
    public static void main(String[] args) {
        int[][] mat={{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}};
        System.out.println(maxhourglasssum(mat));
    }
}
