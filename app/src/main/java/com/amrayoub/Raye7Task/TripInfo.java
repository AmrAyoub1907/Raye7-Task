package com.amrayoub.Raye7Task;

import com.google.android.gms.maps.model.LatLng;

import java.sql.Struct;

/**
 * Created by Amr Ayoub on 6/11/2017.
 * Class for saving trip information
 */

public class TripInfo {
    private static LatLng source;
    private static LatLng destination;
    private static int year;
    private static int month;
    private static int day;
    private static int hour;
    private static int minute;

    public static LatLng getSource() {
        return source;
    }

    public static void setSource(LatLng source) {
        TripInfo.source = source;
    }

    public static LatLng getDestination() {
        return destination;
    }

    public static void setDestination(LatLng destination) {
        TripInfo.destination = destination;
    }

    public static void setDate(int d,int m,int y) {
        TripInfo.day=d;
        TripInfo.month=m;
        TripInfo.year=y;
    }
    public static void setTime(int h,int m) {
        TripInfo.minute=m;
        TripInfo.hour=h;
    }
}
