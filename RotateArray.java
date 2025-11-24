public class RotateArray {
    public static void rotateArray(int arr[],int k){
        int n=arr.length;
        k=k%n;
        if(k==0){
            return;
        }
        swap(arr,0,k-1);
        swap(arr,k,n-1);
        swap(arr,0,n-1);
    }
    public static void swap(int arr[],int l,int r){
        while(l<r){
            int temp=arr[l];
            arr[l]=arr[r];
            arr[r]=temp;
            l++;
            r--;
        }
    }

    public static void rotate(int arr[],int k){
        int n=arr.length;
        k=k%n;
        if(k==0){
            return;
        }
        swap1(0,k-1,arr);
        swap1(k,n-1,arr);
        swap1(0,n-1,arr);
    }
    public static void swap1(int l,int r,int arr[]){
        while(l<r){
            int temp=arr[l];
            arr[l]=arr[r];
            arr[r]=temp;
            r--;
            l++;
        }
    }
    public static void main(String[] args){
        int[] arr={1,2,4,5,6,7,8,2,1,3};
        rotateArray(arr,3);
        for(int i=0;i<arr.length;i++){
            System.out.println(arr[i]);
        }
    }   
}
