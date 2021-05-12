package com.heaven7.android.ldext.manager;

import androidx.lifecycle.LifecycleOwner;

import com.heaven7.android.ldext.livedata.AbsoluteLiveData;
import com.heaven7.android.ldext.observer.FocusDestroyOwner;
import com.heaven7.java.base.util.ArrayUtils;
import com.heaven7.java.base.util.SparseArrayDelegate;
import com.heaven7.java.base.util.SparseFactory;

import java.util.Arrays;
import java.util.List;

public final class ComposeLiveDataManager {

    private final SparseArrayDelegate<Impl> mMap = SparseFactory.newSparseArray(3);

    public int compose(Class<? extends LifecycleOwner>...classes){
        int[] keys = genKeys(classes);
        int key = Arrays.hashCode(keys);
        Impl old = mMap.get(key);
        if(old != null){
            throw new IllegalStateException("can't compose twice.");
        }
        mMap.put(key, new Impl(keys));
        return key;
    }
    @SuppressWarnings("unchecked")
    public <T> AbsoluteLiveData<T> getLiveData(final int key, LifecycleOwner owner){
        final Impl impl = mMap.get(key);
        if(impl == null){
            throw new IllegalStateException("can't find live-data or may be live-data is destroyed");
        }
        if(owner != null){
            if(!impl.hasKey(owner.getClass().getName().hashCode())){
                throw new IllegalStateException("the owner is not register. owner = " + owner);
            }
            impl.start(owner.getClass().getName().hashCode());
            owner.getLifecycle().addObserver(new FocusDestroyOwner() {
                @Override
                protected void onDestroy(LifecycleOwner source) {
                    impl.getLiveData().removeObservers(source);
                    //no lefts
                    if(impl.end(source.getClass().getName().hashCode()) == 0){
                        mMap.remove(key);
                    }
                }
            });
        }
        return (AbsoluteLiveData<T>) impl.getLiveData();
    }
    public <T>AbsoluteLiveData<T> getLiveData(final int key){
        return getLiveData(key, null);
    }
    public boolean hasLiveData(int key){
        return mMap.get(key) != null;
    }

    private static int[] genKeys(Class<?>...classes){
        int[] arr = new int[classes.length];
        for (int i = 0; i < classes.length; i++) {
            arr[i] = classes[i].getName().hashCode();
        }
        return arr;
    }

    private static class Impl{
        final AbsoluteLiveData<?> mEvent = new AbsoluteLiveData<>(true);
        final int[] mKeys;
        final List<Integer> mTempKeys;

        private Impl(int[] mKeys) {
           this.mKeys = mKeys;
           this.mTempKeys = ArrayUtils.toList(mKeys);
        }
        public AbsoluteLiveData<?> getLiveData(){
            return mEvent;
        }
        public void start(int key) {
           if(!mTempKeys.contains(key)){
               mTempKeys.add(key);
           }
        }
        public int end(int key) {
            mTempKeys.remove(key);
            return mTempKeys.size();
        }
        boolean hasKey(int key){
            for (int i : mKeys){
                if(i == key){
                    return true;
                }
            }
            return false;
        }
    }
}
