import java.util.*;

public class PermutationWithRepetition {
    static List<String> list = new ArrayList<>();

    public static void generate(String input, String current, int length) {
        if (current.length() == length) {
            list.add(current);
            return;
        }
        for (int i = 0; i < input.length(); i++) {
            generate(input, current + input.charAt(i), length);
        }
    }
    public static void generate1(String input,String current,int length){
        if(current.length()==length){
            list.add(current);
            return;
        }
        for(int i=0;i<input.length();i++){
            generate1(input, current+input.charAt(i), length);
        }
    }
    public static void main(String[] args) {
        String input = "ABCD";
        int length = input.length(); // 4

        generate(input, "", length);

        for (String s : list) System.out.println(s);
        System.out.println("Total = " + list.size());
    }
}
