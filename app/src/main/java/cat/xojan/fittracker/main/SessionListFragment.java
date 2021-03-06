package cat.xojan.fittracker.main;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;

import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResult;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import cat.xojan.fittracker.BaseFragment;
import cat.xojan.fittracker.Constant;
import cat.xojan.fittracker.R;
import cat.xojan.fittracker.main.controllers.FitnessController;
import cat.xojan.fittracker.main.fragments.sessionlist.DatePickerFragment;
import cat.xojan.fittracker.main.fragments.sessionlist.SessionAdapter;
import cat.xojan.fittracker.util.Utils;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SessionListFragment extends BaseFragment {

    @Inject FitnessController fitController;
    @Inject Context mContext;
    @Inject
    WorkoutFragment workoutFragment;

    @InjectView(R.id.sessions_list) RecyclerView mRecyclerView;
    @InjectView(R.id.date_range_end) Button mDateEndButton;
    @InjectView(R.id.date_range_start) Button mDateStartButton;
    @InjectView(R.id.swipe_container) SwipeRefreshLayout swipeLayout;
    @InjectView(R.id.my_awesome_toolbar) Toolbar toolbar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frament_session_list, container, false);
        ButterKnife.inject(this, view);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);

        swipeLayout.setColorSchemeResources(R.color.accent);
        swipeLayout.setOnRefreshListener(() -> {
            fitController.setEndTime(Calendar.getInstance());
            readSessions();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        readSessions();
        mDateEndButton.setText(Utils.getRightDate(fitController.getEndTime(), getActivity()));
        mDateStartButton.setText(Utils.getRightDate(fitController.getStartTime(), getActivity()));

    }

    @OnClick(R.id.fab_add)
    public void newWorkout(ImageButton imageButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_activity)
                .setItems(ActivityType.getStringArray(getActivity().getBaseContext()), (dialog, which) -> {
                    String activity = ActivityType.values()[which].getActivity();
                    fitController.setFitnessActivity(activity);

                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, workoutFragment, Constant.WORKOUT_FRAGMENT_TAG)
                            .commit();
                });
        builder.create().show();
    }

    @OnClick(R.id.date_range_end)
    public void openEndDateCalendar(Button button) {
        DialogFragment newFragment = new DatePickerFragment() {

            public void onDateSet(DatePicker view, int year, int month, int day) {
                // Do something with the date chosen by the user
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day);
                fitController.setEndTime(calendar);
                button.setText(Utils.getRightDate(fitController.getEndTime(), getActivity()));
                showProgressDialog(true);
                readSessions();
            }
        };
        Bundle bundle = new Bundle();
        bundle.putLong(Constant.PARAMETER_DATE, fitController.getEndTime());
        newFragment.setArguments(bundle);
        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
    }

    @OnClick(R.id.date_range_start)
    public void openStartDateCalendar(Button button) {
        DialogFragment newFragment = new DatePickerFragment() {

            public void onDateSet(DatePicker view, int year, int month, int day) {
                // Do something with the date chosen by the user
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day);
                fitController.setStartTime(calendar);
                button.setText(Utils.getRightDate(fitController.getStartTime(), getActivity()));
                showProgressDialog(true);
                readSessions();
            }
        };
        Bundle bundle = new Bundle();
        bundle.putLong(Constant.PARAMETER_DATE, fitController.getStartTime());
        newFragment.setArguments(bundle);
        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
    }

    private void showProgressDialog(boolean b) {
        if (b) {
            ((MainActivity) getActivity()).showDialog();
        } else {
            ((MainActivity) getActivity()).dismissDialog();
            swipeLayout.setRefreshing(false);
        }
    }

    private void readSessions() {
        showProgressDialog(true);

        Observable.just(fitController.getSessionsReadRequest())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Subscriber<SessionReadRequest>() {

                    private SessionReadResult sessionReadResult;

                    @Override
                    public void onCompleted() {
                        Observable.just(sessionReadResult)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(result -> {
                                    mRecyclerView.setAdapter(new SessionAdapter(mContext, result));
                                    showProgressDialog(false);
                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(Constant.TAG, e.getMessage());
                    }

                    @Override
                    public void onNext(SessionReadRequest sessionReadRequest) {
                        sessionReadResult = Fitness.SessionsApi
                                .readSession(fitController.getClient(), sessionReadRequest)
                                .await(1, TimeUnit.MINUTES);
                    }
                });
    }
}
