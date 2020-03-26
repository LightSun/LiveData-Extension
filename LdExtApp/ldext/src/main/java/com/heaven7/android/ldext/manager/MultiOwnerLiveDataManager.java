package com.heaven7.android.ldext.manager;

import androidx.lifecycle.LifecycleOwner;

import com.heaven7.android.ldext.livedata.SingleLiveEvent;
import com.heaven7.android.ldext.observer.FocusDestroyOwner;
import com.heaven7.java.base.util.ArrayUtils;
import com.heaven7.java.base.util.SparseArrayDelegate;
import com.heaven7.java.base.util.SparseFactory;

import java.util.Arrays;
import java.util.List;

/**
 * the live data manager which hold by use multi owner.
 * the live-data can create by any owner that composed (see {@linkplain #compose(Class[])}).  and destroy on the last destroy LifecycleOwner.
 * @author heaven7
 */
public final class MultiOwnerLiveDataManager {

    private final SparseArrayDelegate<Impl> mMap = SparseFactory.newSparseArray(3);

    /**
     * compose multi owner types to generate a key for them
     * @param classes the owner classes
     * @return the key
     */
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

    /**
     * get the live data by target key and owner
     * @param key the key comes from {@linkplain #compose(Class[])}
     * @param owner the owner
     * @param <T> the data type
     * @return the event.
     */
    @SuppressWarnings("unchecked")
    public <T>SingleLiveEvent<T> getLiveData(final int key, LifecycleOwner owner){
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
        return (SingleLiveEvent<T>) impl.getLiveData();
    }

    /**
     * get the live data by target key
     * @param key the key
     * @param <T> the data type
     * @return the live-data
     */
    public <T>SingleLiveEvent<T> getLiveData(final int key){
        return getLiveData(key, null);
    }

    /**
     * indicate there is a live-data for target key or not
     * @param key the target key
     * @return true if has
     */
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
        final SingleLiveEvent<?> mEvent = new SingleLiveEvent<>(true);
        final int[] mKeys;
        final List<Integer> mTempKeys;

        private Impl(int[] mKeys) {
           this.mKeys = mKeys;
           this.mTempKeys = ArrayUtils.toList(mKeys);
        }
        public SingleLiveEvent<?> getLiveData(){
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
