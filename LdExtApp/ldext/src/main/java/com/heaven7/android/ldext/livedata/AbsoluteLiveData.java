/*
 *  Copyright 2017 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.heaven7.android.ldext.livedata;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbsoluteLiveData<T> extends MutableLiveData<T> {

    private static final Object UNSET = new Object();
    private final AtomicBoolean mPending = new AtomicBoolean(false);
    private final boolean mSticky;
    private Object mValue = UNSET;

    public AbsoluteLiveData(T value, boolean mSticky) {
        super(value);
        this.mValue = value;
        this.mSticky = mSticky;
    }
    public AbsoluteLiveData(T value) {
        this(value, false);
    }

    public AbsoluteLiveData(boolean mSticky) {
        this.mSticky = mSticky;
    }

    public AbsoluteLiveData() {
        this(false);
    }

    @SuppressWarnings("unchecked")
    public <R> AbsoluteLiveData<R> as(Class<R> type){
        return (AbsoluteLiveData<R>) this;
    }

    public AbsoluteLiveData<T> reset(){
        if(mValue != UNSET){
            mValue = UNSET;
        }
        return this;
    }

    @MainThread
    @Override
    public void observe(LifecycleOwner owner, final Observer<? super T> observer) {
       // removeObservers(owner);
        if(mSticky && mValue != UNSET){
            observer.onChanged((T)mValue);
            setDispatchEnabled(false);
           // mPending.set(false);
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

    protected void setDispatchEnabled(boolean enable){
        mPending.set(enable);
    }
    protected boolean shouldDispatchValue(){
        return mPending.get();
    }
    protected boolean isSingle(){
        return false;
    }

    private class WrappedObserver implements Observer<T>{

        private final Observer<? super T> base;

        public WrappedObserver(Observer<? super T> base) {
            this.base = base;
        }
        @Override
        public void onChanged(T t) {
            if(shouldDispatchValue()){
                //single means only re-set value will deliver
                if(isSingle()){
                    setDispatchEnabled(false);
                }
                base.onChanged(t);
            }else {
                //non-single. means recovery
                if(!isSingle()){
                    setDispatchEnabled(true);
                }
            }
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WrappedObserver that = (WrappedObserver) o;
            return Objects.equals(base, that.base);
        }
        @Override
        public int hashCode() {
            return Objects.hash(base);
        }
    }
}
