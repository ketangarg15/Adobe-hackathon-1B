public class Gcd {
    static class Gcdresult{
        int gcd,a,b;
        Gcdresult(int gcd,int a,int b){
            this.gcd=gcd;
            this.a=a;
            this.b=b;
        }
    }
    public static Gcdresult extendgcd(int x,int y){
        if(y==0){
            return new Gcdresult(x,1,0);
        }
        Gcdresult next=extendgcd(y,x%y);
        int a=next.b;
        int b=next.a-(x/y)*next.b;
        return new Gcdresult(next.gcd, a, b);
    }
    public static int gcd(int a,int b){
        return (b==0)?a:gcd(b,a%b);
    }
    public static void main(String[] args){
        System.out.println(gcd(63,2100));
        int x = 30, y = 12;
        Gcdresult res = extendgcd(x, y);
        System.out.println("GCD: " + res.gcd);
        System.out.println("Coefficients: a = " + res.a + ", b = " + res.b);
        System.out.println("Check: " + res.a + "*" + x + " + " + res.b + "*" + y + " = " + (res.a*x + res.b*y));
    }
}
