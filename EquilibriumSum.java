public class EquilibriumSum {
    public static int esum(int arr[]){
        int n=arr.length;
        int[] psum=new int[n];
        int[] ssum=new int[n];
        psum[0]=arr[0];
        for(int i=1;i<n;i++){
            psum[i]=psum[i-1]+arr[i];
        }
        ssum[n-1]=arr[n-1];
        for(int i=n-2;i>=0;i--){
            ssum[i]=ssum[i+1]+arr[i];
        }
        int j=-1;
        int ans=Integer.MIN_VALUE;
        for(int i=0;i<n;i++){
            if(ssum[i]==psum[i]){
                ans=Math.max(psum[i],ans);
                j=i;
            }
        }
        System.out.println(j);
        return (ans==Integer.MIN_VALUE)?-1:ans;
    }
    public static void main(String[] args) {
        int[] arr={1,2,3,5,3,2,1};
        System.out.println(esum(arr));
    }
}
