package sample.calendar;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

public class SampleCalendar extends Activity implements OnClickListener {

	private static String calendarProveName;
	private static String eventsProveName;
	private static String reminderProveName;

	// カレンダーID
	private int[] mCalIds;
	// カレンダー名
	private String[] mCalNames;
	// スピナーで選択されたカレンダーのID
	private int mCalId;

	private int[] mAlertTimes;
	private int mAlertTime;

	private int mStartYear;
	private int mStartMonth;
	private int mStartDay;
	private int mStartHour;
	private int mStartMinute;
	private int mEndYear;
	private int mEndMonth;
	private int mEndDay;
	private int mEndHour;
	private int mEndMinute;

	private int mAlert;
	// 表示用エリア
	private static Spinner mCalendarNameSpinner;
	private static Spinner mAlertNameSpinner;
	private static EditText mTitleText;
	private static EditText mDescriptionText;
	private static EditText mAddressText;
	private static TextView mStartDateButton;
	private static TextView mStartTimeButton;
	private static TextView mEndDateButton;
	private static TextView mEndTimeButton;
	private static Button mAttendButton;

	private static final int START = 1;
	private static final int END = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		initDate();
		mTitleText = (EditText) findViewById(R.id.title);
		mDescriptionText = (EditText) findViewById(R.id.description);
		mAddressText = (EditText) findViewById(R.id.address);
		mStartDateButton = (Button) findViewById(R.id.start_date);
		mStartTimeButton = (Button) findViewById(R.id.start_time);
		mEndDateButton = (Button) findViewById(R.id.end_date);
		mEndTimeButton = (Button) findViewById(R.id.end_time);
		mAttendButton = (Button) findViewById(R.id.attend);
		mCalendarNameSpinner = (Spinner) findViewById(R.id.calendar_spinner);
		mCalendarNameSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						mCalId = mCalIds[position];
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						mCalId = mCalIds[0];
					}
				});

		mAlertNameSpinner = (Spinner) findViewById(R.id.alert_spinner);
		mAlertNameSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						mAlertTime = mAlertTimes[position];
						if (mAlertTime == 0) {
							mAlert = 0;
						} else {
							mAlert = 1;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						mAlertTime = mAlertTimes[0];
					}
				});

		mStartDateButton.setOnClickListener(this);
		mEndDateButton.setOnClickListener(this);
		mStartTimeButton.setOnClickListener(this);
		mEndTimeButton.setOnClickListener(this);
		mAttendButton.setOnClickListener(this);

		mStartDateButton.setText(mStartYear + "/" + (mStartMonth + 1) + "/"
				+ mStartDay);
		mStartTimeButton.setText(mStartHour + ":" + mStartMinute);
		mEndDateButton
				.setText(mEndYear + "/" + (mEndMonth + 1) + "/" + mEndDay);
		mEndTimeButton.setText(mEndHour + ":" + mEndMinute);

		mAlertTimes = getResources().getIntArray(R.array.alert_timevalues);

		setProvName();
		myCalendars2Spinner();

	}

	/**
	 * 日付情報の初期設定
	 */
	private void initDate() {
		Calendar calendar = Calendar.getInstance();
		mStartYear = calendar.get(Calendar.YEAR);
		mStartMonth = calendar.get(Calendar.MONTH);
		mStartDay = calendar.get(Calendar.DAY_OF_MONTH);
		mStartHour = calendar.get(Calendar.HOUR_OF_DAY);
		mStartMinute = calendar.get(Calendar.MINUTE);

		mEndYear = mStartYear;
		mEndMonth = mStartMonth;
		mEndDay = mStartDay;
		mEndHour = mStartHour;
		mEndMinute = mStartMinute;
	}

	/**
	 * イベントをカレンダーに登録する
	 */
	protected void putEvent() {

		final ContentResolver contentResolver = getContentResolver();
		ContentValues cv = new ContentValues();

		/*
		 * イベントの登録
		 */
		cv.put("calendar_id", mCalId);
		cv.put("title", mTitleText.getText().toString());
		cv.put("description", mDescriptionText.getText().toString());
		cv.put("eventLocation", mAddressText.getText().toString());

		Calendar cal = Calendar.getInstance();
		cal.set(mStartYear, mStartMonth, mStartDay, mStartHour, mStartMinute);
		cv.put("dtstart", cal.getTimeInMillis());

		cal.set(mEndYear, mEndMonth, mEndDay, mEndHour, mEndMinute);
		cv.put("dtend", cal.getTimeInMillis());

		// 通知機能を使用する場合は1を設定する
		cv.put("hasAlarm", mAlert);

		Uri uri = contentResolver.insert(Uri.parse(eventsProveName), cv);
		if (mAlert == 1) {

			long eventId = Long.parseLong(uri.getLastPathSegment());

			/*
			 * リマインダー(通知機能)の登録
			 */
			cv.clear();
			cv.put("event_id", eventId);
			cv.put("method", 1);
			// 通知タイミングをイベント開始時間を基準として分単位で指定する
			cv.put("minutes", mAlertTime);
			contentResolver.insert(Uri.parse(reminderProveName), cv);
		}

	}

	/**
	 * SDKバージョンを取得し、各バージョンに当てはまるプロバイダー名を設定する。
	 */
	private void setProvName() {
		// Build.VERSION_CODES
		// 3 CUPCAKE :1.5
		// 4 DONUT :1.6
		// 5 ECLAIR :2.0
		// 6 ECLAIR_0_1 :2.0.1
		// 7 ECLAIR_MR1 :2.1
		// 8 FROYO :2.2

		if (Build.VERSION.SDK_INT < 8) {
			calendarProveName = "content://calendar/calendars";
			eventsProveName = "content://calendar/events";
			reminderProveName = "content://calendar/reminders";
		} else {
			calendarProveName = "content://com.android.calendar/calendars";
			eventsProveName = "content://com.android.calendar/events";
			reminderProveName = "content://com.android.calendar/reminders";
		}
	}

	/**
	 * カレンダー情報をスピナーに設定する
	 */
	private void myCalendars2Spinner() {

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// カレンダー情報を取得しメンバ変数に設定
		getMycalendar();
		int len = mCalIds.length;
		for (int i = 0; i < len; i++) {
			adapter.add(mCalNames[i]);
		}
		mCalendarNameSpinner.setAdapter(adapter);
	}

	/**
	 * ユーザーのカレンダー情報を取得する
	 */
	private void getMycalendar() {

		String[] projection = new String[] { "_id", "name" };

		// 取得条件としてオーナー権限のもののみとする
		// 祝日などのカレンダーはリードオンリーなので取得しないようにする
		// OWNER_ACCESS 700
		// READ_ACCESS 200
		String selection = "access_level" + "=?";
		String[] selectionArgs = new String[] { "700" };

		Uri calendarUri = Uri.parse(calendarProveName);
		Cursor managedCursor = managedQuery(calendarUri, projection, selection,
				selectionArgs, null);

		if (managedCursor != null) {

			// カレンダー情報の取得
			if (managedCursor.moveToFirst()) {

				int len = managedCursor.getCount();
				mCalIds = new int[len];
				mCalNames = new String[len];

				// 各カラムのインデックスを取得
				int idColumnIndex = managedCursor.getColumnIndex("_id");
				int nameColumnIndex = managedCursor.getColumnIndex("name");

				int i = 0;
				do {
					mCalIds[i] = managedCursor.getInt(idColumnIndex);
					mCalNames[i] = managedCursor.getString(nameColumnIndex);
					i++;
				} while (managedCursor.moveToNext());
			}
		}
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.attend:
			putEvent();
			break;
		case R.id.start_date:
			dateSet(START);
			break;
		case R.id.end_date:
			dateSet(END);
			break;
		case R.id.start_time:
			timeSet(START);
			break;
		case R.id.end_time:
			timeSet(END);
			break;
		default:
			break;
		}

	}

	/**
	 * TimePickerDialogを表示する。
	 * 
	 * @param type
	 */
	private void timeSet(int type) {

		OnTimeSetListener onTimeSetListener;
		int hour;
		int minute;

		if (type == START) {
			onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
				@Override
				public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
					mStartHour = hourOfDay;
					mStartMinute = minute;

					mStartTimeButton.setText(mStartHour + ":" + mStartMinute);
				}
			};
			hour = mStartHour;
			minute = mStartMinute;

		} else {
			onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
				@Override
				public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
					mEndHour = hourOfDay;
					mEndMinute = minute;

					mEndTimeButton.setText(mEndHour + ":" + mEndMinute);
				}
			};
			hour = mEndHour;
			minute = mEndMinute;

		}

		final TimePickerDialog timePickerDialog = new TimePickerDialog(this,
				onTimeSetListener, hour, minute, true);
		timePickerDialog.show();

	}

	/**
	 * DatePickerDialogを表示する。
	 * 
	 * @param type
	 */
	private void dateSet(int type) {

		OnDateSetListener onDateSetListener;
		int year;
		int month;
		int day;

		if (type == START) {
			onDateSetListener = new DatePickerDialog.OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					mStartYear = year;
					mStartMonth = monthOfYear;
					mStartDay = dayOfMonth;

					mStartDateButton.setText(mStartYear + "/"
							+ (mStartMonth + 1) + "/" + mStartDay);
				}
			};
			year = mStartYear;
			month = mStartMonth;
			day = mStartDay;

		} else {
			onDateSetListener = new DatePickerDialog.OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					mEndYear = year;
					mEndMonth = monthOfYear;
					mEndDay = dayOfMonth;

					mEndDateButton.setText(mEndYear + "/" + (mEndMonth + 1)
							+ "/" + mEndDay);
				}
			};
			year = mEndYear;
			month = mEndMonth;
			day = mEndDay;

		}

		final DatePickerDialog datePickerDialog = new DatePickerDialog(this,
				onDateSetListener, year, month, day);
		datePickerDialog.show();

	}

}