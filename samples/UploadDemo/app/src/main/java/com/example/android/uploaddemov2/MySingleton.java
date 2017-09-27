package com.example.android.uploaddemov2;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Armand on 9/19/2017.
 */

public class MySingleton {
    private static MySingleton mInstance;
    private RequestQueue mRequestQueue;
    private static Context mCtx;

    private MySingleton(Context context)
    {
        mCtx = context;
        mRequestQueue = getRequestQueue();
    }

    private RequestQueue getRequestQueue()
    {
        if (mRequestQueue == null)
        {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }

        return mRequestQueue;
    }

    public static synchronized MySingleton getInstance (Context context)
    {
        if (mInstance == null)
        {
            mInstance = new MySingleton(context);
        }
        return mInstance;
    }

    public<T> void addToRequestQueue(Request<T> request)
    {
        getRequestQueue().add(request);
    }


}
