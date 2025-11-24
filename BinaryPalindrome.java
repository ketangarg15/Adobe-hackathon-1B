import java.util.*;
public class BinaryPalindrome {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt();
        String num=Integer.toBinaryString(n);
        StringBuilder sb=new StringBuilder(num);
        sb=sb.reverse();
        String reverseString=sb.toString();
        System.out.println(num.equals(reverseString));
    }
}
