package com.reteno.sample.testscreens;

import android.os.Bundle;

import androidx.navigation.NavDirections;

import java.util.Objects;

public class ScreenItem {

    private final String name;
    private final int navigationId;
    private final NavDirections direction;
    private final Bundle bundle;

    public ScreenItem(String name, int navigationId) {
        this.name = name;
        this.navigationId = navigationId;
        this.direction = null;
        this.bundle = null;
    }

    public ScreenItem(String name, NavDirections direction) {
        this.name = name;
        this.navigationId = -1;
        this.direction = direction;
        this.bundle = null;
    }

    public ScreenItem(String name, int navigationId, Bundle bundle) {
        this.name = name;
        this.navigationId = navigationId;
        this.direction = null;
        this.bundle = bundle;
    }

    public String getName() {
        return name;
    }


    public int getNavigationId() {
        return navigationId;
    }

    public NavDirections getDirection() {
        return direction;
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScreenItem that = (ScreenItem) o;
        return navigationId == that.navigationId && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, navigationId);
    }
}
