package com.heaven7.android.ldext.res;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.heaven7.android.ldext.model.Resource;
import com.heaven7.android.ldext.model.Status;
import com.heaven7.java.base.util.Disposable;
import com.heaven7.java.base.util.Scheduler;

import java.util.Objects;

public abstract class NetworkResource<R> {

    private final MediatorLiveData<Resource<R>> mResultLD = new MediatorLiveData<>();
    private final Scheduler mScheduler;
    private volatile Disposable mTask1;

    public NetworkResource(Scheduler mScheduler) {
        this.mScheduler = mScheduler;
    }

    public void cancel(){
        if(mTask1 != null){
            mTask1.dispose();
            mTask1 = null;
        }
    }

    public void start(){
        cancel();
        //loading
        Resource<R> resource = Resource.loading(null);
        setValue(resource);

        fetchFromNetwork();
    }
    private void fetchFromNetwork() {
        final LiveData<Resource<R>> apiResponse = createNetwork();
        mResultLD.addSource(apiResponse, new Observer<Resource<R>>() {
            @Override
            public void onChanged(final Resource<R> response) {
                mResultLD.removeSource(apiResponse);
                if (response.status == Status.SUCCESS) {
                    mTask1 = mScheduler.newWorker().schedule(new Runnable() {
                        @Override
                        public void run() {
                            mTask1 = null;
                            saveNetworkData(response.data);
                            mResultLD.postValue(Resource.success(response.data));
                        }
                    });
                } else if (response.status == Status.ERROR) {
                    mResultLD.postValue(response);
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

    protected abstract LiveData<Resource<R>> createNetwork();

    protected void saveNetworkData(R data){

    }
}
