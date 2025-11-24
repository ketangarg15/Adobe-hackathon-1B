import java.util.*;
public class WeightedString {
    public static int distinctSubString(String str,int k,int N){
        HashSet<String> S=new HashSet<String>();
        for(int i=0;i<N;++i){
            int sum=0;
            String s="";
            for(int j=i;j<N;++j){
                char ch=str.charAt(j);
                int currweight=ch-'a'+1;
                sum+=currweight;
                s+=str.charAt(i);
                if(sum==k){
                    S.add(s);
                }
                else if(sum>k){
                    break;
                }
            }
        }
        return S.size();
    }
    public static void main(String[] args) {
        String str = "abcde";
        int K = 5;
        int N = str.length();
        System.out.print(distinctSubString(str, K, N));
    }
}