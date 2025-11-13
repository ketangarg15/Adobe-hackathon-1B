#include<iostream>
#include<vector>
#include<algorithm>
#include<climits>
using namespace std;
void insertionsort(vector<int>& arr){
    int n=arr.size();
    for(int i=1;i<n;i++){
        int key=arr[i];
        int j=i-1;
        while(j>=0 && arr[j]>key){
            arr[j+1]=arr[j];
            j--;
        }
        arr[j+1]=key;
    }
}
void merge(vector<int>& arr,int l,int h,int m){
    vector<int> temp;
    int i=l;
    int j=m+1;
    int k=0;
    while(i<=m&& j<=h){
        if(arr[i]<arr[j]){
            temp.push_back(arr[i]);
            k++;
            i++;
        }
        else{
            temp.push_back(arr[j]);
            j++;
            k++;
        }
    }
    for(;i<=m;){
        temp.push_back(arr[i++]);
    }
    for(;j<=h;){
        temp.push_back(arr[j++]);
    }
    for(k=0;k<temp.size();k++){
        arr[k+l]=temp[k];
    }
}

void mergeSort(vector<int>& arr,int l,int h){
    if(l>=h){
        return;
    }
    int m=(l+h)/2;
    mergeSort(arr,l,m);
    mergeSort(arr,m+1,h);
    merge(arr,l,h,m);
}
int partition(vector<int>& arr,int l,int h){
    int i=l-1;
    int pivot=arr[h];
    for(int j=l;j<h;j++){
        if(arr[j]<=pivot){
            i++;
            swap(arr[j],arr[i]);
        }
    }
    swap(arr[i+1],arr[h]);
    return (i+1);
}
void quickSort(vector<int>& arr,int l,int h){
    if(h<=l){
        return ;
    }
    int pivot=partition(arr,l,h);
    quickSort(arr,l,pivot-1);
    quickSort(arr,pivot+1,h);
}

struct Result{
    int start;
    int end;
    int sum;
};
struct Result better(Result a,Result b){
    if(a.sum>b.sum){
        return a;
    }
    if(b.sum>a.sum){
        return b;
    }
    int lena=a.end-a.start+1;
    int lenb=b.end-b.start+1;
    return (lena<=lenb)?a:b;
}
Result maxCrossingSum(vector<int>& arr,int l,int mid,int r){
    int leftsum=INT_MIN;
    int sum=0;
    int bestleft=mid;
    int rightsum=INT_MIN;
    for(int i=mid;i>=l;i--){
        sum+=arr[i];
        int len1=mid-i+1;
        int len2=mid-bestleft+1;
        if(sum>leftsum ||(sum==leftsum && len1<len2)){
            leftsum=sum;
            bestleft=i;
        }
    }
    sum=0;
    int bestright=mid+1;
    for(int j=mid+1;j<=r;j++){
        sum+=arr[j];
        int len1=r-j+1;
        int len2=r-bestright+1;
        if(sum>rightsum ||(sum==rightsum && len1<len2)){
            rightsum=sum;
            bestright=j;
        }
    }
    Result res;
    res.sum=rightsum+leftsum;
    res.start=bestleft;
    res.end=bestright;
    return res;
}
Result maxSubarray(vector<int>& arr,int l,int r){
    if(l==r){
        return {l, r, arr[l]};
    }
    int mid=(l+r)/2;
    Result left=maxSubarray(arr,l,mid);
    Result right=maxSubarray(arr,mid+1,r);
    Result cross=maxCrossingSum(arr,l,mid,r);
    return better(better(left,right),cross);
}
struct Activity{
    int id;
    int start;
    int end;
};
bool compare(Activity a,Activity b){
    return a.end<b.end;
}
void ActivitySelection(vector<Activity>& activities){
    int n=activities.size();
    sort(activities.begin(),activities.end(),compare);
    int count=1;
    vector<int> index;
    index.push_back(activities[0].id);
    int lastend=activities[0].end;
    for(int i=1;i<n;i++){
        if(lastend<=activities[i].start){
            count++;
            index.push_back(activities[i].id);
            lastend=activities[i].end;
        }
    }
    for(int i=0;i<count;i++){
        cout<<index[i]<<" ";
    }
}
// insertionsort(arr);
// quickSort(arr,0,arr.size()-1);
// for(int i=0;i<arr.size();i++){
//     cout<<arr[i]<<" ";
// }
//  vector<Activity> activities = {
//     {1, 1, 2},
//     {2, 3, 4},
//     {3, 0, 6},
//     {4, 5, 7},
//     {5, 8, 9},
//     {6, 5, 9}
// };

// // Result r=maxSubarray(arr,0,arr.size()-1);
// // cout<<r.start<<endl;
// // cout<<r.end<<endl<<r.sum;
// ActivitySelection(activities);

int lcs(string& a,string& b){
    int n=a.size();
    int m=b.size();
    vector<vector<int>> dp(n+1,(vector<int>(m+1,0)));
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
            i--;
            j--;
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
int lcsm(string& a,string& b,int i,int j,vector<vector<int>>& dp){
    if(i==0||j==0){
        return 0;
    }
    if(dp[i][j]!=-1){
        return dp[i][j];
    }
    if(a[i-1]==b[j-1]){
        return 1+lcsm(a,b,i-1,j-1,dp);
    }
    else{
        return max(lcsm(a,b,i,j-1,dp),lcsm(a,b,i-1,j,dp));
    }
}

int memom(int i,int j,vector<int>& arr,vector<vector<int>>& dp){
    if(i==j){
        return 0;
    }
    if(dp[i][j]!=-1){
        return dp[i][j];
    }
    int mini=INT_MAX;
    for(int k=i;k<j;k++){
        int cost=arr[i-1]*arr[k]*arr[j]+memom(i,k,arr,dp)+memom(k+1,j,arr,dp);
        mini=min(mini,cost);
    }
    return dp[i][j]=mini;
}

int mcmtab(vector<int>&arr,vector<vector<int>>& split){
    int n=arr.size();
    vector<vector<int>>dp(n,vector<int>(n,0));
    for(int len=2;len<n;len++){
        for(int i=1;i<n-len+1;i++){
            int j=i+len-1;
            dp[i][j]=INT_MAX;
            for(int k=i;k<j;k++){
                int cost=dp[i][k]+dp[k+1][j]+arr[i-1]*arr[j]*arr[k];
                if(cost<dp[i][j]){
                    split[i][j]=k;
                    dp[i][j]=cost;
                }
            }
        }
    }
    return dp[1][n-1];
}
string printMcm(int i,int j,vector<vector<int>>& split){
    if(i==j){
        return "A"+to_string(i-1);
    }
    int k=split[i][j];
    string left=printMcm(i,k,split);
    string right=printMcm(k+1,j,split);
    return "("+left+"*"+right+")";
}

int rcm(int i,int len,vector<int>& length,vector<int>& prices,vector<vector<int>>& dp){
    if(i==0||len==0){
        return 0;
    }
    if(dp[i][len]!=-1){
        return dp[i][len];
    }
    int nottake=rcm(i-1,len,length,prices,dp);
    int take=INT_MIN;
    if(length[i-1]<=len){
        take=rcm(i,len-length[i-1],length,prices,dp)+prices[i-1];
    }
    return dp[i][len]=max(take,nottake);
}
int rctab(int n,vector<int>& length,vector<int>& prices,vector<vector<int>>& parent){
    int m=length.size();
    vector<vector<int>> dp(m+1,vector<int>(n+1,0));
    for(int i=1;i<=m;i++){
        for(int j=1;j<=n;j++){
            int take=0;
            if(j>=length[i-1]){
                take=dp[i][j-length[i-1]]+prices[i-1];
            }
            int nottake=dp[i-1][j];
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
void printproducts(int n,vector<int>& lengths,vector<vector<int>>& parent){
    int i=lengths.size();
    int j=n;
    while(i>0 && j>0){
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

bool isSafe(vector<string>& board,int r,int c,int n){
    for(int i=0;i<r;i++){
        if(board[i][c]=='Q'){
            return false;
        }
    }
    for(int i=r-1,j=c-1;i>=0&&j>=0;i--,j--){
        if(board[i][j]=='Q'){
            return false;
        }
    }
    for(int i=r-1,j=c+1;i>=0&&j<n;i--,j++){
        if(board[i][j]=='Q'){
            return false;
        }
    }
    return true;
}
void solveNQueens(int r,vector<string>& board,vector<vector<string>>& ans,int n){
    if(r==n){
        ans.push_back(board);
        return;
    }
    for(int c=0;c<n;c++){
        if(isSafe(board,r,c,n)){
            board[r][c]='Q';
            solveNQueens(r+1,board,ans,n);
            board[r][c]='.';
        }
    }
}


bool isValid(vector<vector<int>>& board,int r,int c,int n){
    for(int x=0;x<9;x++){
        if(board[r][x]==n||board[x][c]==n){
            return false;
        }
    }
    int startRow=(r/3)*3;
    int startcol=(c/3)*3;
    for(int i=0;i<3;i++){
        for(int j=0;j<3;j++){
            if(board[startRow+i][startcol+j]==n){
                return false;
            }
        }
    }
    return true;
}
bool solveSudoku(vector<vector<int>>& board){
    for(int r=0;r<9;r++){
        for(int c=0;c<9;c++){
            if(board[r][c]==0){
                for(int num=1;num<=9;num++){
                    if(isValid(board,r,c,num)){
                        board[r][c]=num;
                        if(solveSudoku(board)){
                            return true;
                        }
                        board[r][c]=0;
                    }
                }
                return false;
            }
        }
    }
    return true;
}

void naiveStringMatching(string text,string pattern){
    int n=text.length();
    int m=pattern.length();
    for(int i=0;i<=n-m;i++){
        int j;
        for(j=0;j<m;j++){
            if(text[i+j]!=pattern[j]){
                break;
            }
        }
        if(j==m){
            cout<<"Pattern found at index "<<i<<endl;
        }
    }
}
vector<int> buildlps(string& pattern){
    int m=pattern.size();
    int i=1;
    int len=0;
    vector<int> lps(m,0);
    while(i<m){
        if(pattern[i]==pattern[len]){
            len++;
            lps[i]=len;
            i++;
        }
        else{
            if(len!=0){
                len=lps[len-1];
            }
            else{
                lps[i]=0;
                i++;
            }
        }
    }
    return lps;
}
void kmp(string& a,string& b){
    int n=a.size();
    int m=b.size();
    vector<int> lps=buildlps(b);
    int i=0;
    int j=0;
    while(i<n){
        if(a[i]==b[j]){
            i++;
            j++;
        }
        if(j==m){
            cout<<"found at index "<<(i-j)<<endl;
            j=lps[j-1];
        }
        else if(i<n && a[i]!=b[j]){
            if(j!=0){
                j=lps[j-1];
            }
            else{
                i++;
            }
        }
    }
}
#define d 256
#define q 101
void rabinkarp(string& text,string& pattern){
    int n=text.size();
    int m=pattern.size();
    int t=0;
    int p=0;
    int h=1;
    for(int i=0;i<m-1;i++){
        h=(h*d)%q;
    }
    for(int i=0;i<m;i++){
        t=(t*d+text[i])%q;
        p=(p*d+pattern[i])%q;
    }
    for(int i=0;i<=n-m;i++){
        if(p==t){
            bool match=true;
            for(int j=0;j<m;j++){
                if(text[i+j]!=pattern[j]){
                    match=false;
                    break;
                }
            }
            if(match){
                cout<<"found at"<<i<<endl;
            }
        }
        if(i<n-m){
            t=(d*(t-h*text[i])+text[i+m])%q;
            if(t<0){
                t+=q;
            }
        }
    }
}

#include<queue>
#define V 6
bool bfs(vector<vector<int>> &rGraph,int s,int t,vector<int>& parent){
    vector<bool> visited(V,false);
    queue<int> qu;
    qu.push(s);
    visited[s]=true;
    parent[s]=-1;
    while(!qu.empty()){
        int u=qu.front();
        qu.pop();
        for(int v=0;v<V;v++){
            if(!visited[v] && rGraph[u][v]>0){
                qu.push(v);
                parent[v]=u;
                visited[v]=true;
            }
        }
    }
    return visited[t];
}
int fordFulkerson(vector<vector<int>>& graph,int s,int t){
    vector<vector<int>> rGraph=graph;
    vector<int> parent(V);
    int max_flow=0;

    while(bfs(rGraph,s,t,parent)){
        int path_flow=INT_MAX;
        for(int v=t;v!=s;v=parent[v]){
            int u=parent[v];
            path_flow=min(path_flow,rGraph[u][v]);
        }
        for(int v=t;v!=s;v=parent[v]){
            int u=parent[v];
            rGraph[u][v]-=path_flow;
            rGraph[v][u]+=path_flow;
        }
        max_flow+=path_flow;
    }
    return max_flow;
}

int n;
int dist[10][10];
bool visited[10];
int ans = INT_MAX;

void tsp(int city, int count, int cost) {
    if (count == n && dist[city][0] > 0) {
        ans = min(ans, cost + dist[city][0]);
        return;
    }

    for (int i = 0; i < n; i++) {
        if (!visited[i] && dist[city][i] > 0) {
            visited[i] = true;
            tsp(i, count + 1, cost + dist[city][i]);
            visited[i] = false;
        }
    }
}

int main() {
    cout << "Enter number of cities: ";
    cin >> n;

    cout << "Enter distance matrix:\n";
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
            cin >> dist[i][j];

    for (int i = 0; i < n; i++)
        visited[i] = false;

    visited[0] = true; // start from city 0
    tsp(0, 1, 0);

    cout << "Minimum travelling cost: " << ans << endl;
    return 0;
}
int main(){
    // vector<int> arr={10,4,0,-1,-4,9,-19,0,2,5,2,1,5};
    // string a = "ABCBDAB";
    // string b = "BDCABA";
    // cout << "Length of LCS (Tabulation): " << lcs(a, b) << endl;
    // vector<vector<int>> dp(a.size() + 1, vector<int>(b.size() + 1, -1));
    // cout << "Length of LCS (Memoization): " << lcsm(a, b, a.size(), b.size(), dp) << endl;
    // vector<int> arr = {40, 20, 30, 10, 30}; // example dimensions
    // int n = arr.size();
    // vector<vector<int>> dp(n, vector<int>(n, -1));
    // vector<vector<int>> split(n, vector<int>(n, -1));
    // cout << "Minimum cost (Memoization): " << memom(1, n - 1, arr, dp) << endl;
    // int minCost = mcmtab(arr, split);
    // cout << "Minimum cost (Tabulation): " << minCost << endl;
    // cout << "Optimal Parenthesization: " << printMcm(1, n - 1, split) << endl;

    // vector<int> length = {1, 2, 3, 4, 5, 6, 7, 8};
    // vector<int> prices = {1, 5, 8, 9, 10, 17, 17, 20};
    // int n = 8;

    // vector<vector<int>> dp(length.size() + 1, vector<int>(n + 1, -1));
    // vector<vector<int>> parent(length.size() + 1, vector<int>(n + 1, -1));
    // cout << "Max profit (Memoization): " << rcm(length.size(), n, length, prices, dp) << endl;
    // cout << "Max profit (Tabulation): " << rctab(n, length, prices,parent) << endl;
    // printproducts(n,length,parent);
    string text = "AABAACAADAABAAABAA";
    string pattern = "AABA";

    cout << "Naive Matching:\n";
    naiveStringMatching(text, pattern);

    cout << "\nKMP Matching:\n";
    kmp(text, pattern);

    cout << "\nRabin-Karp Matching:\n";
    rabinkarp(text, pattern);
}