import java.util.*;
public class Nqueen {
    public static int N;
    public static void printSolution(int board[][]){
        for(int i=0;i<N;i++){
            for(int j=0;j<N;j++){
                System.out.print(""+board[i][j]+" ");
            }
            System.out.println("");
        }
    }
    public static boolean isSafe(int board[][],int r,int c){
        int i,j;
        for(i=0;i<c;i++){
            if(board[r][i]==1){
                return false;
            }
        }
        for(i=r,j=c;i>=0 && j>=0;i--,j--){
            if(board[i][j]==1){
                return false;
            }
        }
        for(i=r,j=c;j>=0 && i<N;j--,i++){
            if(board[i][j]==1){
                return false;
            }
        }
        return true;
    }
    public static boolean solveNQUtil(int board[][],int col){
        if(col>=N){
            return true;
        }
        for(int i=0;i<N;i++){
            if(isSafe(board,i,col)){
                board[i][col]=1;
                if(solveNQUtil(board, col+1)==true){
                    return true;
                }
                board[i][col]=0;
            }
        }
        return false;
    }
    public static void main(String[] args) {
        N=4;
    int board[][] ={ { 0, 0, 0, 0 },
              { 0, 0, 0, 0 },
              { 0, 0, 0, 0 },
              { 0, 0, 0, 0 } }; 
    if (solveNQUtil(board, 0) == false) {
        System.out.print("Solution does not exist");
    } 
    printSolution(board);
    }
}
