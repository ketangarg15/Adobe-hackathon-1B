import java.util.*;
public class Manchers{
    public static String expand(String s,int l,int r){
        while(l>=0 && r<s.length() && s.charAt(r)==s.charAt(l)){
            l--;
            r++;
        }
        return s.substring(l+1,r);
    }
    public static String longestpalindrome(String s){
        if(s==null || s.length()<1) return "";

        String longest="";
        for(int i=0;i<s.length();i++){
            String p1=expand(s, i, i);
            String p2=expand(s, i, i+1);

            if(p1.length()>longest.length()){
                longest=p1;
            }
            if(p2.length()>longest.length()){
                longest=p2;
            }
        }
        return longest;
    }
    public static void main(String[] args) {
        System.out.println(longestpalindrome("aaaabbbfbbbbabbdbfbejbenem"));
    }
}