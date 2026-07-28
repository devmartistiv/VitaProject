package com.martist.vitamove.exercise.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ExerciseMedia implements Parcelable {
    private String previewImage;
    private String animationUrl;
    private String modelUrl;

    public ExerciseMedia() {
        this(null, null, null);
    }

    public ExerciseMedia(String previewImage, String animationUrl, String modelUrl) {
        this.previewImage = previewImage;
        this.animationUrl = animationUrl;
        this.modelUrl = modelUrl;
    }

    protected ExerciseMedia(Parcel in) {
        previewImage = in.readString();
        animationUrl = in.readString();
        modelUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(previewImage);
        dest.writeString(animationUrl);
        dest.writeString(modelUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ExerciseMedia> CREATOR = new Creator<ExerciseMedia>() {
        @Override
        public ExerciseMedia createFromParcel(Parcel in) {
            return new ExerciseMedia(in);
        }

        @Override
        public ExerciseMedia[] newArray(int size) {
            return new ExerciseMedia[size];
        }
    };

    public String getPreviewImage() {
        return previewImage;
    }

    public String getAnimationUrl() {
        return animationUrl;
    }

    public String getModelUrl() {
        return modelUrl;
    }


    public String getPreviewImageUrl() {
        return previewImage;
    }

    public void setPreviewImage(String previewImage) {
        this.previewImage = previewImage;
    }

    public void setAnimationUrl(String animationUrl) {
        this.animationUrl = animationUrl;
    }

    public void setModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }
} 