package com.heaven7.android.ldext.model;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.lifecycle.LifecycleOwner;

public final class CrossDomainKey implements Parcelable {

    private int originHash;
    private Integer key;

    public static CrossDomainKey from(Intent intent, String keyFromIntent){
        CrossDomainKey key = intent.getParcelableExtra(keyFromIntent);
        if(key == null){
            key = new CrossDomainKey.Builder()
                    .setOriginHash(intent.getIntExtra(keyFromIntent, 0))
                    .build();
        }
        return key;
    }

    public static CrossDomainKey from(LifecycleOwner owner, Integer key){
        return new CrossDomainKey.Builder()
                .setOriginHash(owner.hashCode())
                .setKey(key)
                .build();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.originHash);
        dest.writeValue(this.key);
    }

    protected CrossDomainKey(Parcel in) {
        this.originHash = in.readInt();
        this.key = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    public static final Creator<CrossDomainKey> CREATOR = new Creator<CrossDomainKey>() {
        @Override
        public CrossDomainKey createFromParcel(Parcel source) {
            return new CrossDomainKey(source);
        }

        @Override
        public CrossDomainKey[] newArray(int size) {
            return new CrossDomainKey[size];
        }
    };

    protected CrossDomainKey(CrossDomainKey.Builder builder) {
        this.originHash = builder.originHash;
        this.key = builder.key;
    }

    public int getOriginHash() {
        return this.originHash;
    }

    public Integer getKey() {
        return this.key;
    }

    public static class Builder {
        private int originHash;
        private Integer key;

        public Builder setOriginHash(int originHash) {
            this.originHash = originHash;
            return this;
        }

        public Builder setKey(Integer key) {
            this.key = key;
            return this;
        }

        public CrossDomainKey build() {
            return new CrossDomainKey(this);
        }
    }
}
