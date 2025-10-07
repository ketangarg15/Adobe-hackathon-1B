public class OnesFlip {
    public static int numberof1afterflip(int arr[],int k){
        int n=arr.length;
        int maxOnes=Integer.MIN_VALUE;
        int numReplacements=0;
        int windowstart=0;
        for(int windowend=0;windowend<n;windowend++){
            if(arr[windowend]==0){
                numReplacements++;
            }
            while(numReplacements>k){
                if(arr[windowstart]==0){
                    numReplacements--;
                }
                windowstart++;
            }
            maxOnes=Math.max(maxOnes,windowend-windowstart+1);
        }
        return maxOnes;
    }
    public static void main(String[] args) {
        int[] arr={1,1,0,1,1,1,0,1,1,1,0,1,1};
        System.out.println(numberof1afterflip(arr,1));
    }
}
