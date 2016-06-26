package svenmeier.coxswain.sensors;

import android.Manifest;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import svenmeier.coxswain.HeartSensor;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.util.PermissionBlock;

/**
 * {@link HeartSensor} using the device's sensor.
 */
public class SensorsHeartSensor extends HeartSensor {

	private static final int TYPE_HEART_RATE_LEGACY = 65562;

	private Connection connection;

	private int heartRate = -1;

	public SensorsHeartSensor(Context context, Snapshot memory) {
		super(context, memory);

		connection = new Connection(context);
		connection.open();
	}

	@Override
	public void destroy() {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	@Override
	public void pulse() {
		if (heartRate == -1) {
			return;
		}

		memory.pulse.set(heartRate);
	}

	private class Connection extends PermissionBlock implements SensorEventListener {

		private Sensor sensor;

		public Connection(Context context) {
			super(context);
		}

		public void open() {
			acquirePermissions(Manifest.permission.BODY_SENSORS);
		}

		@Override
		protected void onPermissionsApproved() {
			SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

			sensor = manager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
			if (sensor == null) {
				sensor = manager.getDefaultSensor(TYPE_HEART_RATE_LEGACY);
			}
			if (sensor == null) {
				Toast.makeText(context, R.string.sensors_sensor_no_heart, Toast.LENGTH_LONG).show();
				close();
				return;
			}

			Toast.makeText(context, R.string.sensors_sensor_reading, Toast.LENGTH_LONG).show();
			manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

		public void close() {
			abortPermissions();

			if (sensor != null) {
				SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

				manager.unregisterListener(this);
			}
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.values != null && event.values.length > 0) {
				heartRate = Math.round(event.values[0]);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int i) {
		}
	}
}