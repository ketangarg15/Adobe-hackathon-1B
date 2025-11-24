import java.util.*;

public class CombinationWithRepeat {

    static List<String> result = new ArrayList<>();

    public static void combine(char[] arr, int start, StringBuilder current, int r) {
        if (current.length() == r) {
            result.add(current.toString());
            return;
        }

        for (int i = start; i < arr.length; i++) {
            if (i > start && arr[i] == arr[i - 1]) continue;
            current.append(arr[i]);                  
            combine(arr, i, current, r);            
            current.deleteCharAt(current.length() - 1); 
        }
    }

    public static void combine1(char[] arr,int start,StringBuilder current,int r){
        if(current.length()==r){
            result.add(current.toString());
            return;
        }

        for(int i=start;i<arr.length;i++){
            if(i>start && arr[i]==arr[i-1]){
                continue;
            }
            current.append(arr[i]);
            combine(arr,i,current,r);
            current.deleteCharAt(current.length()-1);
        }
    }
    public static void main(String[] args) {
        String input = "AABC";
        int r = 2;

        char[] arr = input.toCharArray();
        Arrays.sort(arr);

        combine(arr, 0, new StringBuilder(), r);

        for (String s : result) System.out.println(s);
        System.out.println("Total = " + result.size());
    }
}
