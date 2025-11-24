
import java.util.Scanner;

public class Remainder {
    public static void main(String[] args) {
        int n;
        Scanner sc=new Scanner(System.in);
        n=sc.nextInt();
        int[] rem=new int[n];
        int[] div=new int[n];
        System.out.println("Enter the remainders :");
        for(int i=0;i<n;i++){
            rem[i]=sc.nextInt();
        }
        System.out.println("Enter the divisors");
        for(int i=0;i<n;i++){
            div[i]=sc.nextInt();
        }
        int j,x=1;
        while (true) { 
         for(j=0;j<n;j++){
            if(x%div[j]!=rem[j]){
                break;
            }
         }   
         if(j==n){
             System.out.println(x);
             return;
         }
         x++;
        }
    }
}
