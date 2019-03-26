package com.jackchance.yixun.Service.StepCounterService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;


import com.jackchance.yixun.R;

import java.text.DecimalFormat;
import java.util.Calendar;

@SuppressLint("HandlerLeak")
public class StepCounterActivity extends Activity {

	private TextView tv_show_step;//���м���
	private TextView tv_timer;// ����ʱ��
	private TextView tv_distance;// �г�
	private TextView tv_calories;// ��·��
	private TextView tv_velocity;// �ٶ�
	private Button btn_start;// ��ʼ��ť
	private Button btn_stop;// ֹͣ��ť


	private boolean isRun = false;  //�ܲ���������

	private long timer = 0;// �˶�ʱ��
	private  long startTimer = 0;// ��ʼʱ��
	private  long tempTime = 0;

	private Double distance = 0.0;// ·�̣���
	private Double calories = 0.0;// ��������·��
	private Double velocity = 0.0;// �ٶȣ���ÿ��

	private int step_length = 0;  //����
	private int weight = 0;       //����
	private int total_step = 0;   //�ߵ��ܲ���

	private Thread thread;  //�����̶߳���

	private TableRow hide1, hide2;
	private TextView step_counter;

	// ������һ���µ�Handlerʵ��ʱ, ����󶨵���ǰ�̺߳���Ϣ�Ķ�����,��ʼ�ַ�����
	// Handler����������, (1) : ��ʱִ��Message��Runnalbe ����
	// (2): ��һ������,�ڲ�ͬ���߳���ִ��.

	Handler handler = new Handler() {// Handler�������ڸ��µ�ǰ����,��ʱ������Ϣ�����÷�����ѯ����������ʾ��������������������
		//��Ҫ�������̷߳��͵�����, ���ô�����������̸߳���UI
		//Handler���������߳���(UI�߳���), �������߳̿���ͨ��Message��������������, 
		//Handler�ͳе��Ž������̴߳�������(���߳���sendMessage()��������Message����(�����������)
		//����Щ��Ϣ�������̶߳����У�������߳̽��и���UI��

		@Override                  //��������ǴӸ���/�ӿ� �̳й����ģ���Ҫ��дһ��
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);        // �˴����Ը���UI

			countDistance();     //���þ��뷽������һ�����˶�Զ

			if (timer != 0 && distance != 0.0) {

				// ���ء�����
				// �ܲ�������kcal�������أ�kg�������루�����1.036
				calories = weight * distance * 0.001;
				//�ٶ�velocity
				velocity = distance * 1000 / timer;
			} else {
				calories = 0.0;
				velocity = 0.0;
			}
			countStep();          //���ò�������
			tv_show_step.setText(total_step + "");// ��ʾ��ǰ����
			tv_distance.setText(formatDouble(distance));// ��ʾ·��
			tv_calories.setText(formatDouble(calories));// ��ʾ��·��
			tv_velocity.setText(formatDouble(velocity));// ��ʾ�ٶ�
			tv_timer.setText(getFormatTime(timer));// ��ʾ��ǰ����ʱ��
		}


	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_step_count);
        //��ȡ���м����Ĳ���
		if (SettingsActivity.sharedPreferences == null) {
			SettingsActivity.sharedPreferences = this.getSharedPreferences(
					SettingsActivity.SETP_SHARED_PREFERENCES,
					Context.MODE_PRIVATE);
		}
		Bundle extras = getIntent().getExtras();
		isRun = extras.getBoolean("run");

        //����һ���̣߳����������仯
		if (thread == null) {
			thread = new Thread() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					super.run();
					int temp = 0;
					while (true) {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (StepCounterService.FLAG) {
							Message msg = new Message();
							if (temp != StepDetector.CURRENT_SETP) {
								temp = StepDetector.CURRENT_SETP;
							}
							if (startTimer != System.currentTimeMillis()) {
								timer = tempTime + System.currentTimeMillis()
										- startTimer;
							}
							handler.sendMessage(msg);// ֪ͨ���߳�
						}
					}
				}
			};
			thread.start();
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		Log.i("APP", "on resuame.");
		// ��ȡ����ؼ�
		addView();
		// ��ʼ���ؼ�
		init();

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	/**
	 * ��ȡActivity��ؿؼ�
	 */
	private void addView() {
		tv_show_step = (TextView) this.findViewById(R.id.show_step);
		tv_timer = (TextView) this.findViewById(R.id.timer);
		tv_distance = (TextView) this.findViewById(R.id.distance);
		tv_calories = (TextView) this.findViewById(R.id.calories);
		tv_velocity = (TextView) this.findViewById(R.id.velocity);
		btn_start = (Button) this.findViewById(R.id.start);
		btn_stop = (Button) this.findViewById(R.id.stop);
		hide1 = (TableRow)findViewById(R.id.hide1);
		hide2 = (TableRow)findViewById(R.id.hide2);
		step_counter = (TextView)findViewById(R.id.step_counter);
		if(isRun){
			hide1.setVisibility(View.GONE);
			hide2.setVisibility(View.GONE);
			step_counter.setText("����");
		}

		Intent service = new Intent(this, StepCounterService.class);
		stopService(service);
		StepDetector.CURRENT_SETP = 0;
		tempTime = timer = 0;
		tv_timer.setText(getFormatTime(timer));      //����ر�֮�󣬸�ʽ��ʱ��
		tv_show_step.setText("0");
		tv_distance.setText(formatDouble(0.0));
		tv_calories.setText(formatDouble(0.0));
		tv_velocity.setText(formatDouble(0.0));

		handler.removeCallbacks(thread);

	}

	/**
	 * ��ʼ������
	 */
	private void init() {

		step_length = SettingsActivity.sharedPreferences.getInt(
				SettingsActivity.STEP_LENGTH_VALUE, 70);
		weight = SettingsActivity.sharedPreferences.getInt(
				SettingsActivity.WEIGHT_VALUE, 50);

		countDistance();
		countStep();
		if ((timer += tempTime) != 0 && distance != 0.0) {  //tempTime��¼�˶�����ʱ�䣬timer��¼ÿ���˶�ʱ��

			// ���ء�����
			// �ܲ�������kcal�������أ�kg�������루�����1.036������һ��
			calories = weight * distance * 0.001;

			velocity = distance * 1000 / timer;
		} else {
			calories = 0.0;
			velocity = 0.0;
		}

		tv_timer.setText(getFormatTime(timer + tempTime));

		tv_distance.setText(formatDouble(distance));
		tv_calories.setText(formatDouble(calories));
		tv_velocity.setText(formatDouble(velocity));

		tv_show_step.setText(total_step + "");
		btn_start.setEnabled(!StepCounterService.FLAG);

	}


	/**
	 * ���㲢��ʽ��doubles��ֵ��������λ��Ч����
	 * 
	 * @param doubles
	 * @return ���ص�ǰ·��
	 */
	private String formatDouble(Double doubles) {
		DecimalFormat format = new DecimalFormat("####.##");
		String distanceStr = format.format(doubles);
		return distanceStr.equals(getString(R.string.zero)) ? getString(R.string.double_zero)
				: distanceStr;
	}

	public void onClick(View view) {
		Intent service = new Intent(this, StepCounterService.class);
		switch (view.getId()) {
		case R.id.start:
			startService(service);
			btn_start.setEnabled(false);
			btn_stop.setEnabled(true);
			startTimer = System.currentTimeMillis();
			tempTime = timer;
			break;

		case R.id.stop:
            stopService(service);
            btn_start.setEnabled(true);
            Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			break;	
		}
	}

	/**
	 * �õ�һ����ʽ����ʱ��
	 * 
	 * @param time
	 *            ʱ�� ����
	 * @return ʱ���֣��룺����
	 */
	private String getFormatTime(long time) {
		time = time / 1000;
		long second = time % 60;
		long minute = (time % 3600) / 60;
		long hour = time / 3600;

		// ��������ʾ��λ
		// String strMillisecond = "" + (millisecond / 10);
		// ����ʾ��λ
		String strSecond = ("00" + second)
				.substring(("00" + second).length() - 2);
		// ����ʾ��λ
		String strMinute = ("00" + minute)
				.substring(("00" + minute).length() - 2);
		// ʱ��ʾ��λ
		String strHour = ("00" + hour).substring(("00" + hour).length() - 2);

		return strHour + ":" + strMinute + ":" + strSecond;
		// + strMillisecond;
	}

	/**
	 * �������ߵľ���                                                                        
	 */
	private void countDistance() {
		if (StepDetector.CURRENT_SETP % 2 == 0) {
			distance = (StepDetector.CURRENT_SETP / 2) * 3 * step_length * 0.01;
		} else {
			distance = ((StepDetector.CURRENT_SETP / 2) * 3 + 1) * step_length * 0.01;
		}
	}

	/**
	 * ʵ�ʵĲ���
	 */
	private void countStep() {
		if (StepDetector.CURRENT_SETP % 2 == 0) {
			total_step = StepDetector.CURRENT_SETP;
		} else {
			total_step = StepDetector.CURRENT_SETP +1;
		}

		total_step = StepDetector.CURRENT_SETP;
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		finish();
	}

}
