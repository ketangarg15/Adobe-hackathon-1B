import java.util.*;
public class Prime {
    public static void getPrimeNo(int n){
        boolean prime[]=new boolean[n+1];
        for(int p=2;p*p<=n;p++){
            if(!prime[p]){
                for(int i=p*p;i<=n;i+=p){
                    prime[i]=true;
                }
            }
        }

        for(int i=2;i<=n;i++){
            if(prime[i]==false){
                System.out.print(i+" ");
            }
        }
    }
    public static void main(String[] args){
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt();
        getPrimeNo(n);
    }
}
