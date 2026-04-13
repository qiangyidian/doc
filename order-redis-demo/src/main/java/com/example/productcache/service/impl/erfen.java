package com.example.productcache.service.impl;

import java.util.ArrayList;

public class erfen {


        public int erfen(ArrayList<Integer> arr, int a){

            int l=0;
            int r=arr.size()-1;
            int cur = (arr.size()-1)/2;
            while(l<=r){
                if(a<=arr.get(cur)){
                    r=cur;
                }else{
                    l=cur+1;
                }
                cur = (l+r)/2;
            }
            return l;
        }

}
