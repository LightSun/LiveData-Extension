package com.heaven7.android.ldext;

import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.heaven7.android.ldext.livedata.SingleLiveEvent;
import com.heaven7.java.visitor.ResultVisitor;
import com.heaven7.java.visitor.collection.VisitServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ComposeLiveDataHelper {

    private static final String TAG = "ComposeLiveDataHelper";
    private final List<LiveData<?>> mList;
    private SingleLiveEvent<List<?>> mAnyEvent;
    private SingleLiveEvent<List<?>> mAllEvent;

    public ComposeLiveDataHelper() {
        this.mList = new ArrayList<>();
    }
    public ComposeLiveDataHelper with(LiveData<?> data){
        mList.add(data);
        return this;
    }
    public ComposeLiveDataHelper observeAny(LifecycleOwner owner, Observer<List<?>> observer){
        if(mAnyEvent == null){
            mAnyEvent = new SingleLiveEvent<>(true);
        }
        ObserverAny any = new ObserverAny(mAnyEvent);
        for (LiveData<?> data : mList){
            data.observe(owner, any);
        }
        mAnyEvent.observe(owner, observer);
        return this;
    }
    public ComposeLiveDataHelper observeAll(LifecycleOwner owner, Observer<List<?>> observer){
        if(mAllEvent == null){
            mAllEvent = new SingleLiveEvent<>(true);
        }
        ObserverAll any = new ObserverAll(mAllEvent);
        for (LiveData<?> data : mList){
            data.observe(owner, any);
        }
        mAllEvent.observe(owner, observer);
        return this;
    }

    public void removeObservers(LifecycleOwner owner){
        for (LiveData<?> data : mList){
            data.removeObservers(owner);
        }
        if(mAnyEvent != null){
            mAnyEvent.removeObservers(owner);
        }
        if(mAllEvent != null){
            mAllEvent.removeObservers(owner);
        }
    }
    public void reset(){
        mList.clear();
    }

    private class ObserverAny implements Observer<Object>{

        final SingleLiveEvent<List<?>> mEvent;

        private ObserverAny(SingleLiveEvent<List<?>> mEvent) {
            this.mEvent = mEvent;
        }
        @Override
        public void onChanged(Object o) {
            List<Object> data = VisitServices.from(mList).map(new ResultVisitor<LiveData<?>, Object>() {
                @Override
                public Object visit(LiveData<?> data, Object param) {
                    return data.getValue();
                }
            }).getAsList();
            mEvent.postValue(data);
        }
    }
    private class ObserverAll implements Observer<Object>{

        final SingleLiveEvent<List<?>> mEvent;
        final AtomicInteger mCurCount = new AtomicInteger();

        private ObserverAll(SingleLiveEvent<List<?>> mEvent) {
            this.mEvent = mEvent;
        }
        @Override
        public void onChanged(Object o) {
            int size = mList.size();
            if(mCurCount.incrementAndGet() == size){
                if(mCurCount.compareAndSet(size, 0)){
                    List<Object> data = VisitServices.from(mList).map(new ResultVisitor<LiveData<?>, Object>() {
                        @Override
                        public Object visit(LiveData<?> data, Object param) {
                            return data.getValue();
                        }
                    }).getAsList();
                    mEvent.postValue(data);
                }else {
                    Log.d(TAG, "ObserverAll : mCurCount set to 0 failed.");
                }
            }
        }
    }
}
