import java.util.*;
public class Permutation {
    static List<String> list =new ArrayList<>();
    public static void distinctPermutations(String input){
        char[] chars=input.toCharArray();
        Arrays.sort(chars);
        permute(chars,0);
    }
    public static void permute(char[] chars,int index){
        if(index==chars.length-1){
            list.add(String.valueOf(chars));
            return;
        }
        Set<Character> used=new HashSet<>();
        for(int i=index;i<chars.length;i++){
            if(used.contains(chars[i]))continue;
            used.add(chars[i]);
            swap(chars,index,i);
            permute(chars, index+1);
            swap(chars,index,i);
        }
    }
    public static void permute1(char[] chars,int index){
        if(index==chars.length-1){
            list.add(String.valueOf(chars));
            return;
        }
        Set<Character> used=new HashSet<>();
        for(int i=index;i<chars.length;i++){
            if(used.contains(chars[i]))continue;

            used.add(chars[i]);
            swap(chars,index,i);
            permute(chars, index+1);
            swap(chars,index,i);
        }
    }
    public static void swap(char[] chars,int i,int j){
        char temp=chars[i];
        chars[i]=chars[j];
        chars[j]=temp;
    }
    public static void main(String[] args){
        String input="ABCD";
        distinctPermutations(input);
        Collections.sort(list);
        for(String s: list){
            System.out.println(s);
        }
        System.out.println(list.size());
    }
}