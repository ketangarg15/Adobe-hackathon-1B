
import java.util.ArrayList;

public class LeaderinArray {
    public static void getleader(int arr[]){
        int n=arr.length;
        ArrayList<Integer> ans=new ArrayList<>();
        ans.add(arr[n-1]);
        int max=ans.get(0);
        for(int i=n-2;i>=0;i--){
            if(arr[i]>max){
                max=arr[i];
                ans.add(max);
            }
        }
        for(int i=0;i<ans.size();i++){
            System.out.println(ans.get(i));
        }
    }
    public static void main(String[] args) {
        int[] arr={11,4,9,5,11,4,2,8,2,4,7,8};
        getleader(arr);
    }
}
