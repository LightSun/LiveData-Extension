package com.heaven7.android.ldext.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * the resource
 * @param <T> the data type
 */
public class Resource<T> {
    @NonNull
    public final Status status;

    @Nullable
    public final String message;

    @Nullable
    public final int code;

    @Nullable
    public final T data;

    public Resource(@NonNull Status status, @Nullable T data, @Nullable int code, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, 0, null);
    }

    public static <T> Resource<T> error(@Nullable T data) {
        return new Resource<>(Status.ERROR, data, -1, null);
    }

    public static <T> Resource<T> error(@Nullable String message) {
        return new Resource<>(Status.ERROR, null, -1, message);
    }

    public static <T> Resource<T> error(@Nullable int code, @Nullable String message) {
        return new Resource<>(Status.ERROR, null, code, message);
    }

    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, 0, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Resource<?> resource = (Resource<?>) o;

        if (status != resource.status) {
            return false;
        }
        if (message != null ? !message.equals(resource.message) : resource.message != null) {
            return false;
        }
        return data != null ? data.equals(resource.data) : resource.data == null;
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }

    public String toStateString() {
        switch (status){
            case ERROR:
                return "ERROR";
            case LOADING:
                return "LOADING";
            case SUCCESS:
                return "SUCCESS";
        }
        return null;
    }
}
