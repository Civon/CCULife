package org.zankio.cculife.ui.ecourse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.zankio.cculife.CCUService.base.listener.IOnUpdateListener;
import org.zankio.cculife.CCUService.base.source.BaseSource;
import org.zankio.cculife.CCUService.ecourse.Ecourse;
import org.zankio.cculife.CCUService.ecourse.model.Course;
import org.zankio.cculife.CCUService.ecourse.source.remote.CourseListSource;
import org.zankio.cculife.CCUService.ecourse.source.remote.CustomCourseListSource;
import org.zankio.cculife.CCUService.kiki.Kiki;
import org.zankio.cculife.Debug;
import org.zankio.cculife.R;
import org.zankio.cculife.ui.base.BaseFragmentActivity;
import org.zankio.cculife.ui.base.BaseMessageFragment;

public class CourseListFragment extends BaseMessageFragment implements IOnUpdateListener<Course[]>{

    public static Ecourse ecourse;
    private CourseAdapter adapter = null;
    private boolean loading;
    private OnCourseSelectedListener context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            this.context = (OnCourseSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement IGetCourseData");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void fetchCourseList() {
        Log.d("CourseListFragment", "fetch Course List");
        loading = true;
        if (ecourse != null && adapter != null && adapter.getCount() != 0) {
            loading = false;
            adapter.notifyDataSetChanged();
            return;
        }
        ecourse = new org.zankio.cculife.CCUService.ecourse.Ecourse(getContext());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (Debug.debug && preferences.getBoolean("debug_ecourse_custom", false)) {
            int year, term;

            year = Integer.parseInt(preferences.getString("debug_ecourse_year", "-1"));
            term = Integer.parseInt(preferences.getString("debug_ecourse_term", "-1"));
            ecourse.fetch(CustomCourseListSource.TYPE, this, year, term, new Kiki(getContext()));
        } else {
            ecourse.fetch(CourseListSource.TYPE, this);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_courselist, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((BaseFragmentActivity)getActivity()).setSSOService(new org.zankio.cculife.CCUService.portal.service.Ecourse());

        adapter = new CourseAdapter();
        ListView courselist = (ListView)view.findViewById(R.id.list);
        courselist.setAdapter(adapter);
        courselist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                final Course course = (Course) parent.getAdapter().getItem(position);
                context.onCourseSelected(ecourse, course);
            }
        });

        ((BaseFragmentActivity)getActivity()).setMessageView(R.id.list);

        fetchCourseList();

        if (loading)
            message().show("讀取中...", true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.course_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onNext(String type, Course[] courses, BaseSource source) {
        this.loading = false;
        if(courses == null || courses.length == 0) {
            message().show("沒有課程");
            return;
        }

        adapter.setCourses(courses);
        message().hide();
    }

    @Override
    public void onError(String type, Exception err, BaseSource source) {
        this.loading = false;
        message().show(err.getMessage());
    }

    @Override
    public void onComplete(String type) {

    }

    public class CourseAdapter extends BaseAdapter {

        Course[] courses = null;
        private boolean ignore_ecourse_warnning;
        public CourseAdapter() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            this.ignore_ecourse_warnning = preferences.getBoolean("ignore_ecourse_warnning", false);
        }

        public void setCourses(Course[] courses){
            this.courses = courses;
            this.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return courses == null ? 0 : courses.length;
        }

        @Override
        public Object getItem(int position) {
            return courses == null ? null : courses[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = convertView;

            if(view == null) {
                view = inflater.inflate(R.layout.item_course, parent, false);
            }

            Course course = courses[position];
            ((TextView) (view.findViewById(R.id.course_name))).setText(course.name + "");

            ((TextView)view.findViewById(R.id.unread)).setText(String.format("%d", course.notice + course.homework + course.exam));

            if (ignore_ecourse_warnning) {
                view.findViewById(R.id.warring).setBackgroundColor( course.warning ? getResources().getColor(R.color.Red_Course_Warring) : 0);
            }
            return view;
        }
    }

    public interface OnCourseSelectedListener {
        void onCourseSelected(Ecourse ecourse, Course course);
    }
    
}