import java.util.Scanner;

public class Majorityelement{
    public static int getmajorityelement(int arr[]){
        int n=arr.length;
        int i=0;
        int m=-1;
        for(int j=0;j<n;j++){
            if(i==0){
                m=arr[j];
            }
            else if(m==arr[j]){
                i++;
            }
            else{
                i--;
            }
        }
        return m;
    }
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt();
        int[] arr=new int[n];
        for(int i=0;i<n;i++){
            arr[i]=sc.nextInt();
        }
        int value=getmajorityelement(arr);
        System.out.println(value);
    }
}