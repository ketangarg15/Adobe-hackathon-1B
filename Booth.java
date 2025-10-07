import java.util.Scanner;
public class Booth {
  public static int multiply(int n1, int n2) {
        int r = n2;
        int A = n1;
        int P = 0;
        int count = Integer.SIZE;
        while (count > 0) {
            if ((r & 1) == 1) {
                P += A;
            }
            A <<= 1;
            count--;
            r >>= 1;
        }
        return P;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter multiplicand (M): ");
        int M = sc.nextInt();
        System.out.print("Enter multiplier (Q): ");
        int Q = sc.nextInt();

        int result = multiply(M, Q);
        System.out.println("Product = " + result);
    }
}
