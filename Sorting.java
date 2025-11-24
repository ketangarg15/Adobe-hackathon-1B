import java.awt.desktop.QuitEvent;
import java.awt.print.Printable;
import java.util.*;
public class Sorting {
    public static void selectionSort(int arr[]){
        int n=arr.length;
        for(int i=0;i<n-1;i++){
            int minindex=i;
            for(int j=i+1;j<n;j++){
                if(arr[j]<arr[minindex]){
                    minindex=j;
                }
            }

            int temp=arr[i];
            arr[i]=arr[minindex];
            arr[minindex]=temp;
        }
    }
    public  static int partition(int arr[],int low,int high){
        int pivot=arr[high];
        int i=low-1;
        for(int j=low;j<high;j++){
            if(arr[j]<=pivot){
                i++;
                int temp=arr[i];
                arr[i]=arr[j];
                arr[j]=temp;
            }
        }
        int temp=arr[i+1];
        arr[i+1]=arr[high];
        arr[high]=temp;
        return i+1;
    }
    public static void quicksort(int arr[],int l,int h){
        if(l<h){
            int pi=partition(arr, l, h);
            quicksort(arr, l,pi-1);
            quicksort(arr,pi+1, h);
        }
    }
    public static void printArr(int arr[]){
        System.out.println("The Sorted Array is");
        for(int i=0;i<arr.length;i++){
            System.out.print(arr[i]+" ");
        }
        System.out.println("");
    }
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt();
        int arr[]=new int[n];
        System.out.print("Enter all the numbers");
        for(int i=0;i<n;i++){
            arr[i]=sc.nextInt();
        }
        int arr1[]=arr;
        selectionSort(arr);
        System.out.println("hi");
        printArr(arr);
        quicksort(arr1,0,n-1);
        printArr(arr1);
    }
}
