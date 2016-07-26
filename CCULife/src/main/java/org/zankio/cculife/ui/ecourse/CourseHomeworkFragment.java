package org.zankio.cculife.ui.ecourse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.zankio.ccudata.base.model.Response;
import org.zankio.ccudata.ecourse.model.Course;
import org.zankio.ccudata.ecourse.model.CourseData;
import org.zankio.ccudata.ecourse.model.Homework;
import org.zankio.ccudata.ecourse.model.HomeworkData;
import org.zankio.cculife.R;
import org.zankio.cculife.ui.base.BaseMessageFragment;
import org.zankio.cculife.ui.base.IGetCourseData;

import rx.Subscriber;


public class CourseHomeworkFragment extends BaseMessageFragment
        implements AdapterView.OnItemClickListener, IGetLoading {
    private Course course;
    private HomeworkAdapter adapter;
    private boolean loading;
    private IGetCourseData context;
    private boolean loaded;
    private CourseFragment.LoadingListener loadedListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            this.context = (IGetCourseData) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement IGetCourseData");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_course_homework, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        adapter = new HomeworkAdapter();
        ListView list = (ListView) view.findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);

        courseChange(getArguments().getString("id"));
    }

    public void courseChange(String id) {
        course = context.getCourse(id);

        if (course == null) {
            getFragmentManager().popBackStack("list", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return;
        }

        course.getHomework()
                .subscribe(new Subscriber<Response<Homework[], CourseData>>() {
                    @Override
                    public void onCompleted() {
                        setLoaded(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        CourseHomeworkFragment.this.loading = false;
                        setLoaded(true);
                        message().show(e.getMessage());
                    }

                    @Override
                    public void onNext(Response<Homework[], CourseData> courseDataResponse) {
                        CourseHomeworkFragment.this.loading = false;
                        onHomewrokUpdate(courseDataResponse.data());
                    }

                    @Override
                    public void onStart() {
                        setLoaded(false);
                        message().show("讀取中...", true);
                    }
                });
    }

    private Subscriber<Response<Homework, HomeworkData>> homeworkContentListener = new Subscriber<Response<Homework, HomeworkData>>() {
        @Override
        public void onCompleted() { }

        @Override
        public void onError(Throwable e) {
            onHomewrokContentUpdate(e.getMessage(), null);
        }

        @Override
        public void onNext(Response<Homework, HomeworkData> response) {
            onHomewrokContentUpdate(null, response.data());
        }

    };

    private void onHomewrokContentUpdate(String err, Homework homework) {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        TextView message = new TextView(context);
        String content = null, url = null;
        if (err != null)
            content = err;
        else {
            switch (homework.getContentType()) {
                case 1:
                    url = homework.contentUrl;
                    break;
                case 0:
                    content = homework.content;
                    break;
                default:
                    content = "讀取錯誤";
            }
        }

        if (url != null) {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(homework.contentUrl)));
        } else if (content != null) {
            message.setText(Html.fromHtml(content));
            message.setAutoLinkMask(Linkify.WEB_URLS);
            message.setMovementMethod(LinkMovementMethod.getInstance());
            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            message.setPadding(20, 20, 20, 20);
            builder.setView(message);

            //builder.setMessage(Html.fromHtml(announce.getContent()));
            if (homework != null) builder.setTitle(homework.title);
            builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            final AlertDialog dialog = builder.create();
            if (context instanceof Activity && ((Activity)context).isFinishing()) return;
            dialog.show();
        } else {
            Toast.makeText(context, "讀取題目錯誤", Toast.LENGTH_SHORT).show();
        }

    }

    private void onHomewrokUpdate(Homework[] homework) {
        if(homework == null || homework.length == 0) {
            message().show("沒有作業");
            return;
        }

        adapter.setHomeworks(homework);
        message().hide();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Homework homework = (Homework) parent.getAdapter().getItem(position);
        homework.getContent().subscribe(homeworkContentListener);
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
        if (loadedListener != null) loadedListener.call(loaded);
    }

    @Override
    public boolean isLoading() {
        return !this.loaded;
    }

    @Override
    public void setLoadedListener(CourseFragment.LoadingListener listener) {
        this.loadedListener = listener;
    }

    public class HomeworkAdapter extends BaseAdapter {

        private Homework[] homeworks;
        private LayoutInflater inflater;

        public HomeworkAdapter() {
            this.inflater = LayoutInflater.from(getContext());
        }

        public void setHomeworks(Homework[] homework){
            this.homeworks = homework;
            this.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return homeworks == null ? 0 : homeworks.length;
        }

        @Override
        public Object getItem(int position) {
            return homeworks == null ? null : homeworks[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Homework homework = (Homework) getItem(position);

            View view = convertView == null ? inflater.inflate(R.layout.item_homework, null) : convertView;
            ((TextView)view.findViewById(R.id.Title)).setText(homework.title);
            ((TextView)view.findViewById(R.id.Deadline)).setText(homework.deadline);
            ((TextView)view.findViewById(R.id.Score)).setText(homework.score);

            return view;
        }
    }
}
