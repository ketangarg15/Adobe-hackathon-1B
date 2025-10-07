public class SwapNibble {
    public static void main(String[] args) {
        int num=90;
        int swapnum=0;
        swapnum=(num&0x0F)<<4|(num&0xF0)>>4;
        System.out.println(swapnum);
    }
}
