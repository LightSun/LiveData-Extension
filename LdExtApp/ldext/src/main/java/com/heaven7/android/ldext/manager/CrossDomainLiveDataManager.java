package com.heaven7.android.ldext.manager;

import androidx.lifecycle.LifecycleOwner;

import com.heaven7.android.ldext.livedata.SingleLiveEvent;
import com.heaven7.android.ldext.observer.FocusDestroyOwner;
import com.heaven7.java.base.util.SparseArrayDelegate;
import com.heaven7.java.base.util.SparseFactory;

/**
 * the cross domain live data manager. which is create and destroy on origin LifecycleOwner. But can observer on other LifecycleOwner.
 * @author heaven7
 */
public final class CrossDomainLiveDataManager{

    private static final String TAG = "CrossDomainLiveDataM";
    private SparseArrayDelegate<SingleLiveEvent<?>> mMap;
    private SparseArrayDelegate<SparseArrayDelegate<SingleLiveEvent<?>>> mMultiMap;

    /**
     * create cross-domain live data manager.
     */
    public CrossDomainLiveDataManager() {
        this.mMap = SparseFactory.newSparseArray(5);
    }
    /**
     * called on the origin LifecycleOwner start.
     * @param origin the origin LifecycleOwner
     */
    public void start(LifecycleOwner origin){
        SingleLiveEvent<?> liveEvent = mMap.get(origin.hashCode());
        if(liveEvent == null){
            mMap.put(origin.hashCode(), new SingleLiveEvent<Object>(true));
        }
        origin.getLifecycle().addObserver(new FocusDestroyOwner() {
            @Override
            protected void onDestroy(LifecycleOwner source) {
                mMap.remove(source.hashCode());
            }
        });
    }

    /**
     * called on the origin LifecycleOwner start. this used for multi live-data
     * @param origin the origin owner
     * @return the multi object
     */
    public Multi startMulti(LifecycleOwner origin){
        if(mMultiMap == null){
            mMultiMap = SparseFactory.newSparseArray(3);
        }
        SparseArrayDelegate<SingleLiveEvent<?>> delegate = mMultiMap.get(origin.hashCode());
        if(delegate == null){
            delegate = SparseFactory.newSparseArray(3);
            mMultiMap.put(origin.hashCode(), delegate);
        }
        origin.getLifecycle().addObserver(new FocusDestroyOwner() {
            @Override
            protected void onDestroy(LifecycleOwner source) {
                mMultiMap.remove(source.hashCode());
            }
        });
        return new Multi(this, origin.hashCode());
    }
    public <T> SingleLiveEvent<T> getLiveData(LifecycleOwner origin, int key){
        return getLiveData(origin.hashCode(), key);
    }
    @SuppressWarnings("unchecked")
    private <T> SingleLiveEvent<T> getLiveData(int originHash, int key){
        if(mMultiMap == null){
            throw new IllegalStateException("you must call #startMulti(...) first!>");
        }
        SparseArrayDelegate<SingleLiveEvent<?>> delegate = mMultiMap.get(originHash);
        if(delegate == null){
            throw new IllegalStateException("you must call #startMulti(...) first!>");
        }
        SingleLiveEvent<?> event = delegate.get(key);
        if(event == null){
            throw new IllegalStateException("can't find live-data for origin_hash =  " + originHash + " And key = " + key);
        }
        return (SingleLiveEvent<T>) event;
    }
    /**
     * get the live data by original lifecycle owner
     * @param origin the origin LifecycleOwner
     * @param <T> the data type
     * @return the Live data
     */
    @SuppressWarnings("unchecked")
    public <T> SingleLiveEvent<T> getLiveData(LifecycleOwner origin){
        SingleLiveEvent<T> event = (SingleLiveEvent<T>) mMap.get(origin.hashCode());
        if(event == null){
            throw new IllegalStateException("can't find live-data for origin owner: " + origin);
        }
        return event;
    }

    /**
     * get the live data from another LifecycleOwner
     * @param originHash the hash from origin LifecycleOwner
     * @param another the another LifecycleOwner
     * @param <T> the data type
     * @return the live data
     */
    @SuppressWarnings("unchecked")
    public <T> SingleLiveEvent<T> getLiveData(final int originHash, LifecycleOwner another){
        final SingleLiveEvent<T> event = (SingleLiveEvent<T>) mMap.get(originHash);
        if(event == null){
            throw new IllegalStateException("can't find live-data for originHash =  " + originHash);
        }
        another.getLifecycle().addObserver(new FocusDestroyOwner(){
            @Override
            protected void onDestroy(LifecycleOwner source) {
                event.removeObservers(source);
            }
        });
        return event;
    }
    /**
     * get the live data from another LifecycleOwner
     * @param originHash the hash from origin LifecycleOwner
     * @param key the key to get live-data
     * @param another the another LifecycleOwner
     * @param <T> the data type
     * @return the live data
     */
    public <T> SingleLiveEvent<T> getLiveData(final int originHash, int key, LifecycleOwner another){
        final SingleLiveEvent<T> liveData = getLiveData(originHash, key);
        another.getLifecycle().addObserver(new FocusDestroyOwner(){
            @Override
            protected void onDestroy(LifecycleOwner source) {
                liveData.removeObservers(source);
            }
        });
        return liveData;
    }

    public static class Multi{
        final CrossDomainLiveDataManager manager;
        final int hash;

        private Multi(CrossDomainLiveDataManager manager, int hash) {
            this.manager = manager;
            this.hash = hash;
        }
        public void register(int...keys){
            SparseArrayDelegate<SingleLiveEvent<?>> delegate = manager.mMultiMap.get(hash);
            for (int key : keys){
                SingleLiveEvent<?> event = delegate.get(key);
                if(event == null){
                    delegate.put(key, new SingleLiveEvent<>(true));
                }
            }
        }
    }
}
