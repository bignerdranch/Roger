package com.bignerdranch.franklin.roger;

import java.lang.InstantiationException;

import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public abstract class AutoFragmentActivity extends FragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_fragment_activity);

        if (savedInstanceState == null) {
            String fragmentName = getFragmentClassName();
            FragmentManager fm = getSupportFragmentManager();

            try {
                Class<? extends Fragment> fragmentClass = Class.forName(fragmentName)
                    .asSubclass(Fragment.class);
                Fragment f = fragmentClass.newInstance();

                fm.beginTransaction()
                    .add(R.id.autoFragmentContent, f)
                    .commit();
            } catch (InstantiationException ie) {
                throw new RuntimeException("Failed to instantiate fragment", ie);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException("Default constructor not public", iae);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Could not find fragment class", cnfe);
            }
        }
    }

    String getFragmentClassName() {
        String myName = getClass().getName();

        return myName + "$Auto";
    }
}


