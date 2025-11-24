public class Karasuba {
    public static int karatsuba(int num1,int num2){
        if(num1<10 || num2<10){
            return num1*num2;
        }
        int powof10=Math.max(getNumdigit(num1),getNumdigit(num2));
        int m=(int) Math.pow(10,powof10/2);
        int a=num1/m;
        int b=num1%m;
        int c=num2/m;
        int d=num2%m;
        int ac=karatsuba(a,c);
        int bd=karatsuba(b,d);
        int abcd=karatsuba(a+b,c+d);
        int result=ac*m*m+(abcd-ac-bd)*m+bd;
        return result;
    }
    public static int getNumdigit(int num1){
        if(num1==0){
            return 1;
        }
        int count=0;
        while(num1>0){
            count++;
            num1/=10;
        }
        return count;
    }

    public static int multiply(int n1,int n2){
        if(n1<10 ||n2<10){
            return n1*n2;
        }
        int dig=Math.max(getNumdigit(n1),getNumdigit(n2));
        int m=(int)Math.pow(10,dig/2);
        int a = n1/m;
        int b=n1%m;
        int c=n2/m;
        int d=n2%m;
        int ac=karatsuba(a,c);
        int bd=karatsuba(b,d);
        int abcd=karatsuba(a+b,c+d);
        int num=ac*m*m+(abcd-ac-bd)*m+bd;
        return num;
    }
    public static void main(String[] args) {
        System.out.println(multiply(20,29));
    }
}
