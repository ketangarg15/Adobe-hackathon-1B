

public class Abc {
    static int charnum=26;
    static void getfreq(String s,int freq[]){
        int n=s.length();
        for(int i=0;i<n;i++){
            freq[s.charAt(i)-'a']++;
        }
    }
    static boolean canMakePalindrome(int freq[],int len){
        int countodd=0;
        for(int i=0;i<charnum;i++){
            if(freq[i]%2==1){
                countodd++;
            }
        }
        if(len%2==0){
            if(countodd!=0){
                return false;
            }
            else{
                return true;
            }
        }
        if(countodd!=1){
            return false;
        }
        return true;
    }
    public static String oddstr(int freq[]){
        String odd="";
        for(int i=0;i<charnum;i++){
            if(freq[i]%2!=0){
                freq[i]--;
                odd+=(char)(i+'a');
                return odd;
            }
        }
        return odd;
    }
    public static String lexplaindrome(String s){
        int len=s.length();
        int[] freq=new int[charnum];
        getfreq(s, freq);
        if(!canMakePalindrome(freq, len)){
            return "no";
        }

        String rear="";
        String front="";
        String odd=oddstr(freq);
        for(int i=0;i<charnum;i++){
            String temp="";
            if(freq[i]!=0){
                for(int j=1;j<=freq[i]/2;j++){
                    temp+=(char)(i+'a');
                }
                rear=temp+rear;
                front+=temp;
            }
        }
        return (front+odd+rear);
    }
    public static void main(String[] args) {
       System.out.println(lexplaindrome("abybccydzzkkdzxzx"));
    }
}