public class MaxprodSubarray {
    public static int maxprod(int arr[]){
        int n=arr.length;
        if(n==0){
            return 0;
        }
        int minsofar=arr[0];
        int maxsofar=arr[0];
        int result=arr[0];
        for(int i=1;i<n;i++){
            int num=arr[i];
            if(num<0){
                int temp=minsofar;
                minsofar=maxsofar;
                maxsofar=temp;
            }

            maxsofar=Math.max(num,maxsofar*num);
            minsofar=Math.min(num,minsofar*num);
            result=Math.max(result,maxsofar);
        }
        return result;
    }
    public static void main(String[] args) {
        int[] arr={1,2,4,5,0,1,2,4,5,6};
        System.out.println(maxprod(arr));
    }
}
