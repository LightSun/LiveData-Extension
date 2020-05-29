package com.heaven7.android.ldext.manager;

import android.content.Intent;

import androidx.lifecycle.LifecycleOwner;

import com.heaven7.android.ldext.livedata.AbsoluteLiveData;
import com.heaven7.android.ldext.model.CrossDomainKey;
import com.heaven7.android.ldext.observer.FocusDestroyOwner;
import com.heaven7.java.base.util.SparseArrayDelegate;
import com.heaven7.java.base.util.SparseFactory;

/**
 * the cross domain live data manager. which is create and destroy on origin LifecycleOwner. But observer on other LifecycleOwner.
 * @author heaven7
 */
public final class CrossDomainLiveDataManager{

    private static final String TAG = "CrossDomainLiveDataM";
    private SparseArrayDelegate<AbsoluteLiveData<?>> mMap;
    private SparseArrayDelegate<SparseArrayDelegate<AbsoluteLiveData<?>>> mMultiMap;

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
        AbsoluteLiveData<?> liveEvent = mMap.get(origin.hashCode());
        if(liveEvent == null){
            mMap.put(origin.hashCode(), new AbsoluteLiveData<Object>(true));
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
        SparseArrayDelegate<AbsoluteLiveData<?>> delegate = mMultiMap.get(origin.hashCode());
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
    public <T> AbsoluteLiveData<T> getLiveData(LifecycleOwner origin, int key){
        return getLiveData(origin.hashCode(), key);
    }
    @SuppressWarnings("unchecked")
    private <T> AbsoluteLiveData<T> getLiveData(int originHash, int key){
        if(mMultiMap == null){
            throw new IllegalStateException("you must call #startMulti(...) first!>");
        }
        SparseArrayDelegate<AbsoluteLiveData<?>> delegate = mMultiMap.get(originHash);
        if(delegate == null){
            throw new IllegalStateException("you must call #startMulti(...) first!>");
        }
        AbsoluteLiveData<?> event = delegate.get(key);
        if(event == null){
            throw new IllegalStateException("can't find live-data for origin_hash =  " + originHash + " And key = " + key);
        }
        return (AbsoluteLiveData<T>) event;
    }
    /**
     * get the live data by original lifecycle owner
     * @param origin the origin LifecycleOwner
     * @param <T> the data type
     * @return the Live data
     */
    @SuppressWarnings("unchecked")
    public <T> AbsoluteLiveData<T> getLiveData(LifecycleOwner origin){
        AbsoluteLiveData<T> event = (AbsoluteLiveData<T>) mMap.get(origin.hashCode());
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
    public <T> AbsoluteLiveData<T> getLiveData(final int originHash,LifecycleOwner another){
        final AbsoluteLiveData<T> event = (AbsoluteLiveData<T>) mMap.get(originHash);
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
     * @param key the CrossDomainKey
     * @param another the another LifecycleOwner
     * @param <T> the data type
     * @return the live data
     */
    public <T> AbsoluteLiveData<T> getLiveData(CrossDomainKey key, LifecycleOwner another){
        if(key.getKey() == null){
            return getLiveData(key.getOriginHash(), another);
        }else {
            return getLiveData(key.getOriginHash(), key.getKey(), another);
        }
    }
    /**
     * get the live data from another LifecycleOwner
     * @param intent the intent
     * @param keyFromIntent the key from intent
     * @param another the another LifecycleOwner
     * @param <T> the data type
     * @return the live data
     */
    public <T> AbsoluteLiveData<T> getLiveDataFromIntent(Intent intent, String keyFromIntent, LifecycleOwner another){
        CrossDomainKey key = intent.getParcelableExtra(keyFromIntent);
        if(key != null){
            return getLiveData(key, another);
        }else {
            int hash = intent.getIntExtra(keyFromIntent, 0);
            return getLiveData(hash, another);
        }
    }
    /**
     * get the live data from another LifecycleOwner
     * @param originHash the hash from origin LifecycleOwner
     * @param key the key to get live-data
     * @param another the another LifecycleOwner
     * @param <T> the data type
     * @return the live data
     */
    public <T> AbsoluteLiveData<T> getLiveData(final int originHash, int key, LifecycleOwner another){
        final AbsoluteLiveData<T> liveData = getLiveData(originHash, key);
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
            SparseArrayDelegate<AbsoluteLiveData<?>> delegate = manager.mMultiMap.get(hash);
            for (int key : keys){
                AbsoluteLiveData<?> event = delegate.get(key);
                if(event == null){
                    delegate.put(key, new AbsoluteLiveData<>(true));
                }
            }
        }
    }
}
