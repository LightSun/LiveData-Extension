package com.heaven7.android.ldext.res;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.heaven7.android.ldext.model.Resource;
import com.heaven7.android.ldext.model.Status;
import com.heaven7.java.base.util.Disposable;
import com.heaven7.java.base.util.Scheduler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiSourceManager<R> {

    private final MediatorLiveData<Resource<R>> mResultLD = new MediatorLiveData<>();

    private final List<WeakReference<Disposable>> mTasks = new CopyOnWriteArrayList<>();
    private final List<SourceDelegate<R>> mSources = new ArrayList<>();
    private final Scheduler mScheduler;
    private final Callback<R> mCallback;

    private final AtomicInteger mIndex = new AtomicInteger(0);

    public MultiSourceManager(Scheduler mScheduler, Callback<R> callback) {
        this.mScheduler = mScheduler;
        this.mCallback = callback;
    }

    public MultiSourceManager<R> addSourceDelegate(SourceDelegate<R> source){
        mSources.add(source);
        return this;
    }

    public void cancel(){
        for (WeakReference<Disposable> ref : mTasks){
            Disposable d = ref.get();
            if(d != null){
                d.dispose();
            }
        }
        mIndex.set(0);
    }
    protected void addTask(WeakReference<Disposable> ref){
        mTasks.add(ref);
    }
    protected void removeTask(WeakReference<Disposable> ref){
        mTasks.remove(ref);
    }
    public void start(){
        cancel();

        Resource<R> resource = Resource.loading(null);
        setValue(resource);

        next();
    }
    private void next(){
        final SourceDelegate<R> sd = peekSourceDelegate();
        if(sd != null){
            if(sd.shouldAsync()){
                Disposable d = mScheduler.newWorker().schedule(new Runnable() {
                    @Override
                    public void run() {
                        getDataFromSource( sd);
                    }
                });
                addTask(new WeakReference<>(d));
            }else {
                getDataFromSource(sd);
            }
        }else {
            throw new IllegalStateException("shouldn't reach here");
        }
    }

    private void getDataFromSource(final SourceDelegate<R> sd) {
        final LiveData<Resource<R>> ld = sd.getSource();
        mResultLD.addSource(ld, new Observer<Resource<R>>() {
            @Override
            public void onChanged(Resource<R> res) {
                mResultLD.removeSource(ld);
                if(res.status == Status.SUCCESS){
                    if (mCallback.isResourceValid(res.data)) {
                        mCallback.saveData(res.data, sd);
                        mResultLD.addSource(ld, new Observer<Resource<R>>() {
                            @Override
                            public void onChanged(Resource<R> res) {
                                mResultLD.removeSource(ld);
                                mIndex.set(0);
                                setValue(Resource.success(res.data));
                            }
                        });
                    } else {
                        next();
                    }
                }else if(res.status == Status.ERROR){
                    if(mCallback.shouldIgnoreError(res.code, sd)){
                        next();
                    }else {
                        setValue(res);
                    }
                }
            }
        });
    }

    private SourceDelegate<R> peekSourceDelegate() {
        int index = mIndex.getAndIncrement();
        if(mSources.size() > index){
            return mSources.get(index);
        }
        return null;
    }
    private void setValue(final Resource<R> newValue) {
        if (!Objects.equals(mResultLD.getValue(), newValue)) {
            mResultLD.postValue(newValue);
        }
    }
    public abstract static class SourceDelegate<R>{
        public boolean shouldAsync(){
            return false;
        }
        public abstract LiveData<Resource<R>> getSource();
    }
    public abstract static class Callback<R>{

        public boolean isResourceValid(R data){
            return true;
        }
        public boolean shouldIgnoreError(int code, SourceDelegate<R> sd) {
            return false;
        }

        public abstract void saveData(R data, SourceDelegate<R> fromSource);
    }

}
