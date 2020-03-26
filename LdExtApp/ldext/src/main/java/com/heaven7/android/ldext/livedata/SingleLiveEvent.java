package com.heaven7.android.ldext.livedata;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * the live-data which can used as global and sticky .
 * @param <T> the data type
 * @author heaven7
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private static final Object UNSET = new Object();
    private final AtomicBoolean mPending = new AtomicBoolean(false);
    private final boolean mSticky;
    private Object mValue = UNSET;

    public SingleLiveEvent(T value, boolean mSticky) {
        super(value);
        this.mValue = value;
        this.mSticky = mSticky;
    }
    public SingleLiveEvent(T value) {
        this(value, false);
    }

    public SingleLiveEvent(boolean mSticky) {
        this.mSticky = mSticky;
    }

    public SingleLiveEvent() {
        this(false);
    }

    @MainThread
    @Override
    public void observe(LifecycleOwner owner, final Observer<? super T> observer) {
        removeObservers(owner);
        if(mSticky && mValue != UNSET){
            observer.onChanged((T)mValue);
            mPending.set(false);
        }
        WrappedObserver wrappedObserver = new WrappedObserver(observer);
        // Observe the internal MutableLiveData
        super.observe(owner, wrappedObserver);
    }

    @MainThread
    @Override
    public void setValue(@Nullable T t) {
        if(mSticky){
            mValue = t;
        }
        mPending.set(true);
        super.setValue(t);
    }

    @Override
    public void postValue(T value) {
        if(mSticky){
            mValue = value;
        }
        mPending.set(true);
        super.postValue(value);
    }

    private class WrappedObserver implements Observer<T>{

        private final Observer<? super T> base;

        public WrappedObserver(Observer<? super T> base) {
            this.base = base;
        }

        @Override
        public void onChanged(T t) {
            if (mPending.compareAndSet(true, false)) {
                base.onChanged(t);
            }
        }
    }
}
