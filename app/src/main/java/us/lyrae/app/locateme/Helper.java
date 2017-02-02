package us.lyrae.app.locateme;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;

/**
 * Created by zerocchi on 30/01/2017.
 */

class Helper {

    // Thanks to this answer: http://stackoverflow.com/a/4981063/2783365
    static void getAddressFromLocation(
            final Location location, final Context context, final Handler handler) {

        Thread thread = new Thread() {
            @Override public void run() {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                String result = null;
                Address address = null;
                try {
                    List<Address> list = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1);
                    if (list != null && list.size() > 0) {
                        address = list.get(0);
                        // sending back first address line and locality
                        result = address.getAddressLine(0) + "\n"
                                + address.getLocality() + "\n"
                                + address.getPostalCode() + " "
                                + (address.getSubAdminArea() == null ? "" : address.getSubAdminArea() + "\n")
                                + address.getAdminArea() + "\n"
                                + address.getCountryName();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Can't connect to Geocoder", e);
                    address = null;
                } finally {
                    Message msg = Message.obtain();
                    msg.setTarget(handler);
                    if (result != null) {
                        msg.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("address", address);
                        msg.setData(bundle);
                    } else
                        msg.what = 0;
                    msg.sendToTarget();
                }
            }
        };
        thread.start();
    }


    // Thanks to this answer: http://stackoverflow.com/a/28391810/2783365
    static ArrayList<String> getFormattedLocationInDegree(double latitude, double longitude) {
        ArrayList<String> coordinatesDMS = new ArrayList<>();
        try {
            int latSeconds = (int) Math.round(latitude * 3600);
            int latDegrees = latSeconds / 3600;
            latSeconds = Math.abs(latSeconds % 3600);
            int latMinutes = latSeconds / 60;
            latSeconds %= 60;

            int longSeconds = (int) Math.round(longitude * 3600);
            int longDegrees = longSeconds / 3600;
            longSeconds = Math.abs(longSeconds % 3600);
            int longMinutes = longSeconds / 60;
            longSeconds %= 60;
            String latDegree = latDegrees >= 0 ? "N" : "S";
            String lngDegrees = longDegrees >= 0 ? "E" : "W";

            coordinatesDMS.add(Math.abs(latDegrees) + "°" + latMinutes + "'" + latSeconds
                    + "\" " + latDegree);
            coordinatesDMS.add(Math.abs(longDegrees) + "°" + longMinutes + "'" + longSeconds +
                    "\" " + lngDegrees);

        } catch (Exception e) {
            coordinatesDMS.add(String.format("%8.5f", latitude));
            coordinatesDMS.add(String.format("%8.5f", longitude));
        }

        return coordinatesDMS;
    }

}
