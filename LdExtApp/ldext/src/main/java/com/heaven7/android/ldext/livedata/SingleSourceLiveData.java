package com.heaven7.android.ldext.livedata;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

public class SingleSourceLiveData<T, R> extends MutableLiveData<R> {

    private LiveData<T> lastSource;
    private T lastData;

    private final Observer<T> observer = new Observer<T>() {
        @Override
        public void onChanged(T t) {
            if (t != null && t == lastData) {
                return;
            }
            lastData = t;
            setValue(mFunc.apply(t));
        }
    };
    private Function<T, R> mFunc;

    public void setMapFunction(Function<T, R> func) {
        this.mFunc = func;
    }

    public void setSource(LiveData<T> source) {
        if (lastSource == source) {
            return;
        }

        if (lastSource != null) {
            lastSource.removeObserver(observer);
        }
        lastSource = source;

        if (hasActiveObservers()) {
            lastSource.observeForever(observer);
        }
    }

    @Override
    protected void onActive() {
        super.onActive();

        if (lastSource != null) {
            lastSource.observeForever(observer);
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        if (lastSource != null) {
            lastSource.removeObserver(observer);
        }
    }
}

