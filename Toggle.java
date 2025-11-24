import java.util.Scanner;

public class Toggle {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt();
        boolean bulb[]=new boolean[n+1];
        for(int i=1;i<n+1;i++){
            for(int j=i;j<=n;j+=i){
                bulb[j]=!bulb[j];
            }
        }
        int count=0;
        for(int i=1;i<=n;i++){
            if(bulb[i]==true){
                count++;
            }
        }
        System.out.println(count);
    }
}
