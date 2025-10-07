public class Palindrome {
    static int maxchar=26;
    public static void countfreq(String s,int freq[],int len){
        for(int i=0;i<len;i++){
            freq[s.charAt(i)-'a']++;
        }
    }
    public static boolean canMakePalindrome(int freq[],int len){
        int count_odd=0;
        for(int i=0;i<maxchar;i++){
            if(freq[i]%2!=0){
                count_odd++;
            }
        }
        if(len%2==0){
            if(count_odd>0){
                return false;
            }
            else{
                return true;
            }
        }
        if(count_odd!=1){
            return false;
        }
        return true;
    }
    public static String findoddandremoveitsfreq(int freq[]){
        String odd_str="";
        for(int i=0;i<maxchar;i++){
            if(freq[i]%2!=0){
                freq[i]--;
                odd_str=odd_str+(char)(i+'a');
                return odd_str;
            }
        }
        return odd_str;
    }
    public static String findpalindromicString(String str){
        int len=str.length();
        int freq[]=new int[maxchar];
        countfreq(str, freq, len);
        if(!canMakePalindrome(freq, len)){
            return "No palindrome";
        }
        String oddstr=findoddandremoveitsfreq(freq);
        String front="",rear="";
        for(int i=0;i<maxchar;i++){
            String temp="";
            if(freq[i]!=0){
                char ch=(char)(i+'a');
                for(int j=1;j<=freq[i]/2;j++){
                    temp+=ch;
                }
                front+=temp;
                rear=temp+rear;
            }
        }
        return (front+oddstr+rear);
    }
    public static void main(String[] args){
        System.out.println(findpalindromicString("abcdedcba"));
    }
}
