import java.util.*;

public class CombinationNoRepeat {

    static List<String> result = new ArrayList<>();

    public static void combine(char[] arr, int start, StringBuilder current, int r) {
        if (current.length() == r) {
            result.add(current.toString());
            return;
        }

        for (int i = start; i < arr.length; i++) {
            // Skip duplicates
            if (i > start && arr[i] == arr[i - 1]) continue;

            current.append(arr[i]);                      // choose
            combine(arr, i + 1, current, r);            // explore
            current.deleteCharAt(current.length() - 1); // backtrack
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
            combine1(arr,i+1,current,r);
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
