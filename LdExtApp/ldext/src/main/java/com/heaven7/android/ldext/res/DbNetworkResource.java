package com.heaven7.android.ldext.res;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.heaven7.android.ldext.model.Resource;
import com.heaven7.android.ldext.model.Status;
import com.heaven7.java.base.util.Disposable;
import com.heaven7.java.base.util.Scheduler;

import java.util.Objects;

/**
 * the multi source
 * @param <R> the result data type
 * @param <N> the network response data type.
 */
public abstract class DbNetworkResource<R, N> {

    private final MediatorLiveData<Resource<R>> mResultLD = new MediatorLiveData<>();
    private final Scheduler mScheduler;
    private volatile Disposable mTask1;
    private volatile Disposable mTask2;

    public DbNetworkResource(Scheduler mScheduler) {
        this.mScheduler = mScheduler;
    }

    public void cancel(){
        if(mTask1 != null){
            mTask1.dispose();
            mTask1 = null;
        }
        if(mTask2 != null){
            mTask2.dispose();
            mTask2 = null;
        }
    }

    public void start(){
        cancel();
        //loading
        Resource<R> resource = Resource.loading(null);
        setValue(resource);

        mTask1 = mScheduler.newWorker().schedule(new Runnable() {
            @Override
            public void run() {
                final LiveData<R> dbSource = new MutableLiveData<R>(loadFromDb());
                mResultLD.addSource(dbSource, new Observer<R>() {
                    @Override
                    public void onChanged(R data) {
                        mResultLD.removeSource(dbSource);
                        if (shouldFetch(data)) {
                            fetchFromNetwork(dbSource);
                        } else {
                            mResultLD.addSource(dbSource, new Observer<R>() {
                                @Override
                                public void onChanged(R newData) {
                                    setValue(Resource.success(newData));
                                }
                            });
                        }
                    }
                });
                mTask1 = null;
            }
        });
    }

    private void fetchFromNetwork(final LiveData<R> dbSource){
        final LiveData<Resource<N>> network = createNetwork();
        mResultLD.addSource(dbSource, new Observer<R>() {
            @Override
            public void onChanged(R newData) {
                setValue(Resource.loading(newData));
            }
        });
        mResultLD.addSource(network, new Observer<Resource<N>>() {
            @Override
            public void onChanged(final Resource<N> res) {
                //remove source after network success.
                mResultLD.removeSource(network);
                mResultLD.removeSource(dbSource);
                if (res.status == Status.SUCCESS) {
                    mTask2 = mScheduler.newWorker().schedule(new Runnable() {
                        @Override
                        public void run() {
                            saveNetworkData(map(res.data));
                            final LiveData<R> src = new MutableLiveData<R>(loadFromDb());
                            mResultLD.addSource(src, new Observer<R>() {
                                @Override
                                public void onChanged(R newData) {
                                    setValue(Resource.success(newData));
                                }
                            });
                            mTask2 = null;
                        }
                    });
                }else if(res.status == Status.ERROR){
                    onFetchFailed();
                    mResultLD.addSource(dbSource, new Observer<R>() {
                        @Override
                        public void onChanged(R newData) {
                            setValue(Resource.error(newData));
                        }
                    });
                }
            }
        });
    }
    public LiveData<Resource<R>> asLiveData() {
        return mResultLD;
    }

    private void setValue(final Resource<R> newValue) {
        if (!Objects.equals(mResultLD.getValue(), newValue)) {
            mResultLD.postValue(newValue);
        }
    }
    protected boolean shouldFetch(R data) {
        return true;
    }
    protected void onFetchFailed() {
    }
    protected abstract R loadFromDb();

    protected abstract R map(N data);

    protected abstract LiveData<Resource<N>> createNetwork();

    protected abstract void saveNetworkData(R data);
}
