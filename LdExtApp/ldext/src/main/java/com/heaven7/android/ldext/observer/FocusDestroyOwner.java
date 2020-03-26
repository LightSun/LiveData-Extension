package com.heaven7.android.ldext.observer;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

public abstract class FocusDestroyOwner implements LifecycleEventObserver {

    @Override
    public final void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            if (source instanceof Activity) {
                if (!((Activity) source).isChangingConfigurations()) {
                    source.getLifecycle().removeObserver(this);
                    onDestroy(source);
                }
            } else if (source instanceof Fragment) {
                FragmentActivity activity = ((Fragment) source).getActivity();
                if (activity == null || !activity.isChangingConfigurations()) {
                    source.getLifecycle().removeObserver(this);
                    onDestroy(source);
                }
            }else {
                source.getLifecycle().removeObserver(this);
                onDestroy(source);
            }
        }
    }

    protected abstract void onDestroy(LifecycleOwner source);
}
