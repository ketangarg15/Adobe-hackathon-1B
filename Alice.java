
import java.util.Scanner;

public class Alice {
    public static int minApples(int M,int K,int N,int S,int W,int E){
        if(M<=S*K){
            return M;
        }
        else if(M<=S*K+E+W){
            return S*K+(M-S*K);
        }
        else{
            return -1;
        }
    }
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt();
        int cnt=0,sum=0;
        while(sum<n){
            cnt++;
            sum+=12*cnt*cnt;
        }
        System.out.println(8*cnt);
    }
}
