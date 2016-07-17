package org.zankio.cculife.ui.ScoreQuery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.zankio.cculife.CCUService.sourcequery.model.Grade;

public class ScoreDataFragment extends Fragment {
    private static final String TAG_SCORE_DATA_FRAGMENT = "SCORE_DATA_FRAGMENT";
    private Grade[] grades;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setGrades(Grade[] grades) {
        this.grades = grades;
    }

    public Grade[] getGrades() {
        return this.grades;
    }

    public static ScoreDataFragment getFragment(FragmentManager fragmentManager) {
        ScoreDataFragment fragment;
        fragment = (ScoreDataFragment) fragmentManager.findFragmentByTag(TAG_SCORE_DATA_FRAGMENT);
        if (fragment == null) {
            fragment = new ScoreDataFragment();
            fragmentManager.beginTransaction()
                    .add(fragment, TAG_SCORE_DATA_FRAGMENT)
                    .commit();
        }
        return fragment;
    }

}