#include<iostream>
#include<algorithm>
#include<vector>
#include<climits>
using namespace std;

int rodcuttingmemo(int i,int len,vector<int>& lengths,vector<int>& prices,vector<vector<int>>& dp){
    if(i==0||len==0){
        return 0;
    }
    if(dp[i][len]!=-1){
        return dp[i][len];
    }
    
    int nottake=rodcuttingmemo(i-1,len,lengths,prices,dp);
    int take=INT_MIN;
    if(lengths[i-1]<=len){
        take=rodcuttingmemo(i,len-lengths[i-1],lengths,prices,dp)+prices[i-1];
    }
    return dp[i][len]=max(take,nottake);
}
int rocuttingtab(int n,vector<int>& lengths,vector<int>& prices,vector<vector<int>>& parent){
    int m=lengths.size();
    vector<vector<int>> dp(m+1,vector<int>(n+1,0));
    for(int i=1;i<=m;i++){
        for(int j=1;j<=n;j++){
            int take=0;
            if (lengths[i-1] <= j) {
                take = prices[i-1] + dp[i][j - lengths[i-1]];
            }
            int nottake = dp[i-1][j];
            if (take > nottake) {
                dp[i][j] = take;
                parent[i][j] = i;  
            } else {
                dp[i][j] = nottake;
                parent[i][j] = parent[i-1][j];
            }
        }
    }
    return dp[m][n];
}
void printrodcuts(int n,vector<int>& lengths,vector<vector<int>>& parent){
    int i=lengths.size();
    int j=n;
    while(j>0 && i>0){
        if(parent[i][j]==i){
            cout<<lengths[i-1]<<" ";
            j-=lengths[i-1];
        }
        else{
            i--;
        }
    }    
    cout<<endl;
}
int lcsm(int i,int j,string& a,string& b,vector<vector<int>>& dp){
    if(i==0||j==0){
        return 0;
    }
    if(dp[i][j]!=-1){
        return dp[i][j];
    }
    if(a[i-1]==b[j-1]){
        return dp[i][j]=1+lcsm(i-1,j-1,a,b,dp);
    }
    else{
        return dp[i][j]=max(lcsm(i-1,j,a,b,dp),lcsm(i,j-1,a,b,dp));
    }
}
int lcs(string& a,string& b){
    int n=a.size();
    int m=b.size();
    vector<vector<int>> dp(n+1,vector<int>(m+1,0));
    for(int i=1;i<=n;i++){
        for(int j=1;j<=m;j++){
            if(a[i-1]==b[j-1]){
                dp[i][j]=1+dp[i-1][j-1];
            }
            else{
                dp[i][j]=max(dp[i-1][j],dp[i][j-1]);
            }
        }
    }
    string lcs="";
    int i=n,j=m;
    while(i>0 && j>0){
        if(a[i-1]==b[j-1]){
            lcs.push_back(a[i-1]);
            i--;j--;
        }
        else if(dp[i-1][j]>dp[i][j-1]){
            i--;
        }
        else{
            j--;
        }
    }
    reverse(lcs.begin(),lcs.end());
    cout<<lcs;
    return dp[n][m];
}
int mcmMemo(int i,int j,vector<int>& arr,vector<vector<int>>& dp){
    if(i==j){
        return 0;
    }
    if(dp[i][j]!=-1){
        return dp[i][j];
    }
    int mini=INT_MAX;
    for(int k=i;k<j;k++){
        int steps=arr[i-1]*arr[k]*arr[j]+mcmMemo(i,k,arr,dp)+mcmMemo(k+1,j,arr,dp);
        mini=min(mini,steps);
    }
    return dp[i][j]=mini;
}
int mcmTab(vector<int>& arr,vector<vector<int>>& split){
    int n=arr.size();
    vector<vector<int>> dp(n,vector<int>(n,0));
    for(int len=2;len<n;len++){
        for(int i=1;i<n-len+1;i++){
            int j=i+len-1;
            dp[i][j]=INT_MAX;
            for(int k=i;k<j;k++){
                 int cost = arr[i-1]*arr[k]*arr[j] + dp[i][k] + dp[k+1][j];
                if (cost < dp[i][j]) {
                    dp[i][j] = cost;
                    split[i][j] = k; 
                }
            }
        }
    }
    return dp[1][n-1];
}
string printMCM(int i,int j,vector<vector<int>>& split){
    if(i==j){
        return "A"+to_string(i-1);
    }
    int k=split[i][j];
    string left=printMCM(i,k,split);
    string right=printMCM(k+1,j,split);
    return "("+left+"*"+right+")";
}
int main() {
    vector<int> lengths = {1, 2, 3, 4, 5, 6, 7, 8};
    vector<int> prices  = {1, 5, 8, 9, 10, 17, 17, 20};
    int n = 8;
    vector<vector<int>> dp(lengths.size()+1,vector<int>(n+1,-1));
    vector<vector<int>> parent(lengths.size()+1, vector<int>(n+1,0));
    int profit = rocuttingtab(n, lengths, prices, parent);
    cout << "Max profit (Tabulation): " <<profit << endl;
    printrodcuts(n, lengths, parent);
    cout << "Max profit (memo): " << rodcuttingmemo(lengths.size(),n,lengths,prices,dp) << endl;
    
    string a="KetanGarg";
    string b="KGag";
    vector<vector<int>> dp1(a.size()+1,vector<int>(b.size()+1,-1));
    cout<<"LCS Memo:"<<lcsm(a.size(),b.size(),a,b,dp1)<<endl;
    cout<<"LCS:"<<lcs(a,b)<<endl;

    vector<int> arr = {40, 20, 30, 10, 30};
    int l = arr.size();
    vector<vector<int>> split(l, vector<int>(l,0));
    int cost = mcmTab(arr, split);
    cout << "MCM min cost: " << cost << endl;
    cout << "Optimal parenthesization: " << printMCM(1,l-1,split) << endl;
    vector<vector<int>> dp3(l, vector<int>(l, -1));
    cout << "MCM min cost (Memoization): " << mcmMemo(1, l - 1, arr, dp3) << endl;
   
    return 0;
}
