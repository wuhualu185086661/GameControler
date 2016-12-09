package com.wuguowei.control;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	/**
	 * �����Ƿ����ж�����ı���
	 */
	private SensorManager sm;
	private Sensor aSensor;
	private Sensor mSensor;
	float[] accelerometerValues = new float[3];
	float[] magnetticFiledValues = new float[3];
	private TextView direction;
	private String TEMP_DIR = "";
	private String LAST_DIR = "";
	private String FINAL_DIR = "��ʱδ�ҵ�����";
	private Timer timer;
	private int COUNT_DIR = 0;
	private int COUNT_CHANGE = 0;

	/**
	 * �����ǲ����ж�����ı���
	 */
	public static float average = 0;
	// �ϴδ�������ֵ
	float gravityOld = 0;
	// �˴β����ʱ��
	long timeOfThisPeak = 0;
	// �ϴβ����ʱ��
	long timeOfLastPeak = 0;
	// ��ǰ��ʱ��
	long timeOfNow = 0;
	// ����ֵ
	float peakOfWave = 0;
	// ����ֵ
	float valleyOfWave = 0;
	// ��ʼ��ֵ
	float ThreadValue = (float) 2.0;
	// �Ƿ������ı�־λ
	boolean isDirectionUp = false;
	// ��һ���״̬�����������½�
	boolean lastStatus = false;
	// ������������
	int continueUpCount = 0;
	// ��һ��ĳ��������Ĵ�����Ϊ�˼�¼�������������
	int continueUpFormerCount = 0;
	// ��̬��ֵ��Ҫ��̬�����ݣ����ֵ������Щ��̬���ݵ���ֵ
	final float initialValue = (float) 1.7;
	// �����������
	final int valueNum = 5;
	// ���ڴ�ż�����ֵ�Ĳ��岨�Ȳ�ֵ
	float[] tempValue = new float[valueNum];
	int tempCount = 0;
	// ��ӡҪ�õ���tag
	private final String TAG = "StepDcretor";
	public static int TEMP_STEP = 0;
	public static int CURRENT_SETP = 0;
	private TextView count;
	private TextView valuesTV;
	private TextView change;

	/**
	 * �����Ƕ�λ��������ı���
	 */
	private TextView XYZ;
	private String Final_XYZ = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		direction = (TextView) findViewById(R.id.direction);
		sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		count = (TextView) findViewById(R.id.count);
		valuesTV = (TextView) findViewById(R.id.values);
		XYZ = (TextView) findViewById(R.id.XYZ);
		change = (TextView) findViewById(R.id.change);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// ���ٶȴ�����
		aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// �ų�������
		mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (aSensor != null) {
			sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_UI);
			Toast.makeText(MainActivity.this, "���ٶȴ�����ע��ɹ�", 0).show();
		} else {
			Toast.makeText(this, "�����ֻ���֧�ּ��ٶȴ�����", Toast.LENGTH_LONG).show();
		}
		if (mSensor != null) {
			sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_UI);
			Toast.makeText(MainActivity.this, "�ų���Ӧ��ע��ɹ�", 0).show();
		} else {
			Toast.makeText(MainActivity.this, "�����ֻ���֧�ִų���Ӧ��", 0).show();
		}
		time();
		calculateOrientation();
	}

	// �����activity����ͣ��ʱ��ע��������
	@Override
	protected void onPause() {
		sm.unregisterListener(myListener);
		timer.cancel();
		super.onPause();
	}

	private void calculateOrientation() {
		float[] values = new float[3];
		float[] R = new float[9];
		SensorManager.getRotationMatrix(R, null, accelerometerValues, magnetticFiledValues);
		SensorManager.getOrientation(R, values);
		// ������ת���ɶ���
		values[0] = (float) Math.toDegrees(values[0]);
		valuesTV.setText(String.valueOf(values[0]));
		if (values[0] >= -45 && values[0] < 45) {
			TEMP_DIR = "��";
		} else if (values[0] >= 45 && values[0] < 135) {
			TEMP_DIR = "��";
		} else if (values[0] >= 135 && values[0] < 180 || values[0] >= -180 && values[0] < -135) {
			TEMP_DIR = "��";
		} else if (values[0] >= -135 && values[0] < -45) {
			TEMP_DIR = "��";
		}
	}

	public void time() {
		timer = new Timer(true);
		TimerTask task = new TimerTask() {
			public void run() {
				if (LAST_DIR == "" || LAST_DIR != TEMP_DIR) {
					LAST_DIR = TEMP_DIR;
					COUNT_CHANGE++;
					// �����if��Ϊ������΢С�ڶ��ʹ�������żȻ���
					if (COUNT_CHANGE >= 5) {
						FINAL_DIR = "��ǰ�������ʱ�䲻��2��";						
						COUNT_DIR = 0;
						CURRENT_SETP = 0;
					}
				} else {
					COUNT_DIR++;
					COUNT_CHANGE = 0;
					LAST_DIR = TEMP_DIR;
					if (COUNT_DIR >= 20) {
						COUNT_DIR = 0;
						FINAL_DIR = TEMP_DIR;
					}
				}

			}
		};
		// schedule(��Ҫѭ��ִ�е�����,�ӳٶ�ÿ�ʼִ��,ÿ���೤ʱ��ִ��һ��)
		timer.schedule(task, 0, 100);
	}

	final SensorEventListener myListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent sensorEvent) {
			XYZ.setText(Final_XYZ);
			direction.setText(FINAL_DIR);
			count.setText(String.valueOf(CURRENT_SETP));
			change.setText(String.valueOf(COUNT_CHANGE));
			if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
				magnetticFiledValues = sensorEvent.values.clone();
			synchronized (this) {
				if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
					accelerometerValues = sensorEvent.values.clone();
					calc_step(sensorEvent);
				}
			}

		}

		synchronized private void calc_step(SensorEvent event) {
			average = (float) Math
					.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
			DetectorNewStep(average);
		}

		public void DetectorNewStep(float values) {
			if (gravityOld == 0.0) {
				gravityOld = values;
			} else {
				// DetectorPeak(1,2)�������ڼ�Ⲩ�� 1:��ǰ��ֵ 2:��һ���ֵ �������true�����õ��ǲ���
				if (DetectorPeak(values, gravityOld)) {
					timeOfLastPeak = timeOfThisPeak;
					timeOfNow = System.currentTimeMillis();
					if (timeOfNow - timeOfLastPeak >= 100 && (peakOfWave - valleyOfWave >= ThreadValue)
							&& timeOfNow - timeOfLastPeak <= 2000) {
						timeOfThisPeak = timeOfNow;
						// ���²���
						CURRENT_SETP++;
						if (CURRENT_SETP >= 1) {
							CURRENT_SETP = 0;
							COUNT_DIR = 0;
							if (FINAL_DIR == "��")
								Final_XYZ = "��";
							else if (FINAL_DIR == "��")
								Final_XYZ = "��";
							else if (FINAL_DIR == "��")
								Final_XYZ = "��";
							else
								Final_XYZ = "��";
						}
					}
					// initialValue:��̬��ֵ��Ҫ��̬�����ݣ����ֵ������Щ��̬���ݵ���ֵ
					if (timeOfNow - timeOfLastPeak >= 100 && (peakOfWave - valleyOfWave >= initialValue)) {
						timeOfThisPeak = timeOfNow;
						// Peak_Valley_Thread()����������ֵ�ļ���
						ThreadValue = Peak_Valley_Thread(peakOfWave - valleyOfWave);
					}
				}
			}

			calculateOrientation();
			gravityOld = values;
		}

		public boolean DetectorPeak(float newValue, float oldValue) {
			// isDerectionUp:�Ƿ������ı�־λ;lastStatus:��һ���״̬�����������½�
			lastStatus = isDirectionUp;
			if (newValue >= oldValue) {
				isDirectionUp = true;
				// ������������
				continueUpCount++;
			} else {
				// continueUpFormerCount����һ��ĳ��������Ĵ�����Ϊ�˼�¼�������������
				continueUpFormerCount = continueUpCount;
				// ������������
				continueUpCount = 0;
				isDirectionUp = false;
			}
			if (!isDirectionUp && lastStatus && (continueUpFormerCount >= 2 && (oldValue >= 11f && oldValue < 19.6f))) {
				peakOfWave = oldValue;
				return true;
			} else if (!lastStatus && isDirectionUp) {
				valleyOfWave = oldValue;
				return false;
			} else {
				return false;
			}
		}

		public float Peak_Valley_Thread(float value) {
			float tempThread = ThreadValue;
			if (tempCount < valueNum) {
				tempValue[tempCount] = value;
				tempCount++;
			} else {
				tempThread = averageValue(tempValue, valueNum);
				for (int i = 1; i < valueNum; i++) {
					tempValue[i - 1] = tempValue[i];
				}
				tempValue[valueNum - 1] = value;
			}
			return tempThread;

		}

		public float averageValue(float value[], int n) {
			float ave = 0;
			for (int i = 0; i < n; i++) {
				ave += value[i];
			}
			ave = ave / valueNum;
			if (ave >= 8) {
				Log.v(TAG, "����8");
				ave = (float) 4.3;
			} else if (ave >= 7 && ave < 8) {
				Log.v(TAG, "7-8");
				ave = (float) 3.3;
			} else if (ave >= 4 && ave < 7) {
				Log.v(TAG, "4-7");
				ave = (float) 2.3;
			} else if (ave >= 3 && ave < 4) {
				Log.v(TAG, "3-4");
				ave = (float) 2.0;
			} else {
				Log.v(TAG, "else");
				ave = (float) 1.7;
			}
			return ave;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

	};

}
