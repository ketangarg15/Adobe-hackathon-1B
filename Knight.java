import java.util.*;
public class Knight {
    private static final int[] Row_moves={2,1,-1,-2,-2,-1,1,2};
    private static final int[] Col_moves={1,2,2,1,-1,-2,-2,-1};

    private static boolean isSafe(int[][] board,int row,int col,int N){
        return (row>=0 && row<N &&col>=0 && col<N && board[row][col]==-1);
    }
    public static void printSolution(int[][] board){
        int N=board.length;
        for(int[] row:board){
            for(int cell:row){
                System.out.print(cell+" ");
            }
            System.out.println("");
        }
    }
    public static boolean solveKnightTour(int N){
        int[][] board=new int[N][N];
        for(int i=0;i<N;i++){
            for(int j=0;j<N;j++){
                board[i][j]=-1;
            }
        }
        board[0][0]=0;
        if(solveUtil(board,0,0,1,N)){
            printSolution(board);
            return true;
        }
        else{
            System.out.println("No solution exists");
            return false;
        }
    }
    public static boolean solveUtil(int[][] board,int row,int col,int movecount,int N){
        if(movecount==N*N){
            return true;
        }
        for (int i = 0; i < 8; i++) {
            int nextRow = row + Row_moves[i];
            int nextCol = col + Col_moves[i];

            if (isSafe(board, nextRow, nextCol, N)) {
                board[nextRow][nextCol] = movecount;

                if (solveUtil(board, nextRow, nextCol, movecount + 1, N)) {
                    return true;
                }

                // Backtrack
                board[nextRow][nextCol] = -1;
            }
        }
        return false;
    }
    public static void main(String[] args) {
        int N = 8; // Board size
        solveKnightTour(N);
    }
}