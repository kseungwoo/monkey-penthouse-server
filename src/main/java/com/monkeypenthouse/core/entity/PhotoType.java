package com.monkeypenthouse.core.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PhotoType {
    BANNER, DETAIL, CAROUSEL;

    @JsonCreator
    public static PhotoType from(String s) {
        return PhotoType.valueOf(s.toUpperCase());
    }
}
