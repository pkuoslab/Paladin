package com.sei.bean.Collection;

public class Tuple2<A, B> {
    private A a;
    private B b;
    public Tuple2(A a, B b){
        this.a = a;
        this.b = b;
    }

    public A getFirst(){
        return a;
    }

    public B getSecond(){
        return b;
    }


}
