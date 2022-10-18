package com.muzima.adapters.observations;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.muzima.view.fragments.patient.ChronologicalObsViewFragment;
import com.muzima.view.fragments.patient.TabularObsViewFragment;

public class ObsViewAdapter extends FragmentPagerAdapter {
    private final String patientUuid;
    private final Integer totalTabs;
    private final Context context;

    public ObsViewAdapter(@NonNull FragmentManager fm, Integer totalTabs, String patientUuid, Context context) {
        super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.totalTabs = totalTabs;
        this.patientUuid = patientUuid;
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 1)
                return TabularObsViewFragment.newInstance(patientUuid, context);
            else
                return ChronologicalObsViewFragment.newInstance(patientUuid, false);
    }

    @Override
    public int getCount() {
        return totalTabs;
    }
}