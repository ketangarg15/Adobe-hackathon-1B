import java.lang.reflect.Array;
import java.util.*;
public class Strobogrammatic {
    public static List<String> findStrobogrammaitc(int n){
        return helper(n,n);
    }
    private static List<String> helper(int n,int total){
        if(n==0){
            return new ArrayList<>(Arrays.asList(""));
        }
        if(n==1){
            return new ArrayList<>(Arrays.asList("1","8","0"));
        }
        List<String> list=helper(n-2, total);
        List<String> res=new ArrayList<>();
        for(String s:list){
            if(n!=total){
                res.add("0"+s+"0");
            }
            res.add("8"+s+"8");
            res.add("1"+s+"1");
            res.add("9"+s+"6");
            res.add("6"+s+"9");
        }
        return res;
    }
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        int k=sc.nextInt();
        List<String> ans=findStrobogrammaitc(k);
        for(int i=0;i<ans.size();i++){
            System.out.print(ans.get(i));
            if(i<ans.size()-1){
                System.out.print(" ");
            }
        }
        String num=sc.next();
        HashMap<Character,Character> map=new HashMap<>();
        map.put('9','6');
        map.put('6','9');
        map.put('0','0');
        map.put('1','1');
        map.put('8','8');
        int length=num.length();
        int i=0,j=length-1;
        while(i<=j){
            char numleft=map.getOrDefault(num.charAt(i),'-');
            char numright=map.getOrDefault(num.charAt(j),'*');
            if(numleft!=numright){
                System.out.println("break");
                return;
            }
            i++;
            j--;
        }
        System.out.println("correct");
    }
}

