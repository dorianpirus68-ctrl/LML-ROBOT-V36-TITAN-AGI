package com.lml.control;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;

public class TrainingActivity extends Activity {
    public static final String KEY_SCENARIOS = "supervised_scenarios";
    private SharedPreferences preferences;
    private EditText applicationInput;
    private EditText objectiveInput;
    private EditText stepsInput;
    private TextView scenarioSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText(R.string.training_title);
        title.setTextSize(24);
        layout.addView(title);

        TextView description = new TextView(this);
        description.setText(R.string.training_description);
        description.setPadding(0, 16, 0, 16);
        layout.addView(description);

        applicationInput = new EditText(this);
        applicationInput.setHint(R.string.training_application_hint);
        layout.addView(applicationInput);

        objectiveInput = new EditText(this);
        objectiveInput.setHint(R.string.training_objective_hint);
        layout.addView(objectiveInput);

        stepsInput = new EditText(this);
        stepsInput.setHint(R.string.training_steps_hint);
        stepsInput.setMinLines(3);
        layout.addView(stepsInput);

        Button save = new Button(this);
        save.setText(R.string.training_save);
        save.setOnClickListener(view -> saveScenario());
        layout.addView(save, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button schedule = new Button(this);
        schedule.setText(R.string.training_schedule);
        schedule.setOnClickListener(view -> scheduleReminder());
        layout.addView(schedule, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scenarioSummary = new TextView(this);
        scenarioSummary.setPadding(0, 20, 0, 0);
        layout.addView(scenarioSummary);

        setContentView(layout);
        updateSummary();
    }

    private void saveScenario() {
        String application = applicationInput.getText().toString().trim();
        String objective = objectiveInput.getText().toString().trim();
        String steps = stepsInput.getText().toString().trim();
        if (application.isEmpty() || objective.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, R.string.training_required, Toast.LENGTH_LONG).show();
            return;
        }
        NexusScenarioStore.add(this, application, objective, steps);
        applicationInput.setText("");
        objectiveInput.setText("");
        stepsInput.setText("");
        updateSummary();
        Toast.makeText(this, R.string.training_saved, Toast.LENGTH_SHORT).show();
    }

    private void scheduleReminder() {
        String objective = objectiveInput.getText().toString().trim();
        if (objective.isEmpty()) {
            Toast.makeText(this, R.string.training_schedule_need_objective, Toast.LENGTH_LONG).show();
            return;
        }
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.set(Calendar.HOUR_OF_DAY, hour);
            reminderTime.set(Calendar.MINUTE, minute);
            reminderTime.set(Calendar.SECOND, 0);
            if (reminderTime.before(Calendar.getInstance())) {
                reminderTime.add(Calendar.DATE, 1);
            }
            Intent reminderIntent = new Intent(this, TaskReminderReceiver.class);
            reminderIntent.setAction(TaskReminderReceiver.ACTION_REMINDER);
            reminderIntent.putExtra(TaskReminderReceiver.EXTRA_OBJECTIVE, objective);
            int requestCode = (int) (System.currentTimeMillis() & 0xFFFFFFF);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = getSystemService(AlarmManager.class);
            if (alarmManager != null) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), pendingIntent);
                Toast.makeText(this, getString(R.string.training_scheduled,
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(reminderTime.getTime())), Toast.LENGTH_LONG).show();
            }
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
    }

    private void updateSummary() {
        scenarioSummary.setText(getString(R.string.training_count, getScenarioCount(this)));
    }

    public static int getScenarioCount(Activity activity) {
        SharedPreferences preferences = activity.getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        return NexusScenarioStore.count(activity);
    }
}
