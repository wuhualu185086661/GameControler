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
	 * 这里是方向判断引入的变量
	 */
	private SensorManager sm;
	private Sensor aSensor;
	private Sensor mSensor;
	float[] accelerometerValues = new float[3];
	float[] magnetticFiledValues = new float[3];
	private TextView direction;
	private String TEMP_DIR = "";
	private String LAST_DIR = "";
	private String FINAL_DIR = "暂时未找到方向";
	private Timer timer;
	private int COUNT_DIR = 0;
	private int COUNT_CHANGE = 0;

	/**
	 * 这里是步数判断引入的变量
	 */
	public static float average = 0;
	// 上次传感器的值
	float gravityOld = 0;
	// 此次波峰的时间
	long timeOfThisPeak = 0;
	// 上次波峰的时间
	long timeOfLastPeak = 0;
	// 当前的时间
	long timeOfNow = 0;
	// 波峰值
	float peakOfWave = 0;
	// 波谷值
	float valleyOfWave = 0;
	// 初始阈值
	float ThreadValue = (float) 2.0;
	// 是否上升的标志位
	boolean isDirectionUp = false;
	// 上一点的状态，上升还是下降
	boolean lastStatus = false;
	// 持续上升次数
	int continueUpCount = 0;
	// 上一点的持续上升的次数，为了记录波峰的上升次数
	int continueUpFormerCount = 0;
	// 动态阈值需要动态的数据，这个值用于这些动态数据的阈值
	final float initialValue = (float) 1.7;
	// 存放三轴数据
	final int valueNum = 5;
	// 用于存放计算阈值的波峰波谷差值
	float[] tempValue = new float[valueNum];
	int tempCount = 0;
	// 打印要用到的tag
	private final String TAG = "StepDcretor";
	public static int TEMP_STEP = 0;
	public static int CURRENT_SETP = 0;
	private TextView count;
	private TextView valuesTV;
	private TextView change;

	/**
	 * 这里是二位坐标引入的变量
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
		// 加速度传感器
		aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// 磁场传感器
		mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (aSensor != null) {
			sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_UI);
			Toast.makeText(MainActivity.this, "加速度传感器注册成功", 0).show();
		} else {
			Toast.makeText(this, "您的手机不支持加速度传感器", Toast.LENGTH_LONG).show();
		}
		if (mSensor != null) {
			sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_UI);
			Toast.makeText(MainActivity.this, "磁场感应器注册成功", 0).show();
		} else {
			Toast.makeText(MainActivity.this, "您的手机不支持磁场感应器", 0).show();
		}
		time();
		calculateOrientation();
	}

	// 当这个activity被暂停的时候，注销传感器
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
		// 把数据转换成度数
		values[0] = (float) Math.toDegrees(values[0]);
		valuesTV.setText(String.valueOf(values[0]));
		if (values[0] >= -45 && values[0] < 45) {
			TEMP_DIR = "北";
		} else if (values[0] >= 45 && values[0] < 135) {
			TEMP_DIR = "东";
		} else if (values[0] >= 135 && values[0] < 180 || values[0] >= -180 && values[0] < -135) {
			TEMP_DIR = "南";
		} else if (values[0] >= -135 && values[0] < -45) {
			TEMP_DIR = "西";
		}
	}

	public void time() {
		timer = new Timer(true);
		TimerTask task = new TimerTask() {
			public void run() {
				if (LAST_DIR == "" || LAST_DIR != TEMP_DIR) {
					LAST_DIR = TEMP_DIR;
					COUNT_CHANGE++;
					// 下面的if是为了屏蔽微小摆动和传感器的偶然误差
					if (COUNT_CHANGE >= 5) {
						FINAL_DIR = "当前方向持续时间不足2秒";						
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
		// schedule(需要循环执行的任务,延迟多久开始执行,每隔多长时间执行一次)
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
				// DetectorPeak(1,2)方法用于检测波峰 1:当前的值 2:上一点的值 如果返回true表明该点是波峰
				if (DetectorPeak(values, gravityOld)) {
					timeOfLastPeak = timeOfThisPeak;
					timeOfNow = System.currentTimeMillis();
					if (timeOfNow - timeOfLastPeak >= 100 && (peakOfWave - valleyOfWave >= ThreadValue)
							&& timeOfNow - timeOfLastPeak <= 2000) {
						timeOfThisPeak = timeOfNow;
						// 更新步数
						CURRENT_SETP++;
						if (CURRENT_SETP >= 1) {
							CURRENT_SETP = 0;
							COUNT_DIR = 0;
							if (FINAL_DIR == "北")
								Final_XYZ = "上";
							else if (FINAL_DIR == "东")
								Final_XYZ = "右";
							else if (FINAL_DIR == "南")
								Final_XYZ = "下";
							else
								Final_XYZ = "左";
						}
					}
					// initialValue:动态阈值需要动态的数据，这个值用于这些动态数据的阈值
					if (timeOfNow - timeOfLastPeak >= 100 && (peakOfWave - valleyOfWave >= initialValue)) {
						timeOfThisPeak = timeOfNow;
						// Peak_Valley_Thread()方法用于阈值的计算
						ThreadValue = Peak_Valley_Thread(peakOfWave - valleyOfWave);
					}
				}
			}

			calculateOrientation();
			gravityOld = values;
		}

		public boolean DetectorPeak(float newValue, float oldValue) {
			// isDerectionUp:是否上升的标志位;lastStatus:上一点的状态，上升还是下降
			lastStatus = isDirectionUp;
			if (newValue >= oldValue) {
				isDirectionUp = true;
				// 持续上升次数
				continueUpCount++;
			} else {
				// continueUpFormerCount：上一点的持续上升的次数，为了记录波峰的上升次数
				continueUpFormerCount = continueUpCount;
				// 持续上升次数
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
				Log.v(TAG, "超过8");
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
