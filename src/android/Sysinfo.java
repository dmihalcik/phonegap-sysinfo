package com.ankamagames.plugins.sysinfo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.os.Build;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.Process;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import android.util.*;

public class Sysinfo extends CordovaPlugin {
	private static final String TAG = "Sysinfo";
	private static final boolean ALWAYS_GET_CPU = false;
	private MemoryInfo memoryInfo;
	private JSONObject cpuInfo;
	
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callback) {

		Activity activity = this.cordova.getActivity();
		ActivityManager m = (ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE);
		
		// store the memory info into a field.
		if (null == this.memoryInfo){
			this.memoryInfo = new MemoryInfo();
		}
		m.getMemoryInfo(this.memoryInfo);

		// Store the CPU info into a field. Only do this the first time
		if (null == this.cpuInfo || ALWAYS_GET_CPU) {
			cpuInfo = this.getCpuInfo();
		}
		
		if (action.equals("getInfo")) {
			try {
				JSONObject r = new JSONObject();
	            r.put("cpu", this.cpuInfo);
	            r.put("memory", this.getMemoryInfo());
	            Log.v(TAG, r.toString());
	            callback.success(r);
			} catch (final Exception e) {
	            Log.e(TAG, "getInfo failed", e);
				callback.error(e.getMessage());
			}
		}
		
		return false;
	}
	
	public JSONObject getCpuInfo() {
		JSONObject cpu = new JSONObject();
		try {
			// Get CPU Core count
			String output = readSystemFile("/sys/devices/system/cpu/present");
			String[] parts = output.split("-");
			Integer cpuCount = Integer.parseInt(parts[1]) + 1;
			
			cpu.put("count", cpuCount);

			// Get CPU Core frequency
			JSONArray cpuCores = new JSONArray();
			for(int i = 0; i < cpuCount; i++) {
				Integer cpuMaxFreq = getCPUFrequencyMax(i);
				cpuCores.put(cpuMaxFreq == 0 ? null : cpuMaxFreq);
			}
			
			cpu.put("cores", cpuCores);
			
		} catch (final Exception e) {
            Log.w(TAG, "getInfo unable to retrieve CPU details", e);
		}
		return cpu;
	}
	
	public JSONObject getMemoryInfo() {
		JSONObject memory = new JSONObject();
		Runtime runtime = Runtime.getRuntime();
		try {
			memory.put("available", this.memoryInfo.availMem);
			memory.put("total", this.getTotalMemory());
			memory.put("threshold", this.memoryInfo.threshold);
			memory.put("low", this.memoryInfo.lowMemory);
			memory.put("free", runtime.freeMemory());
			memory.put("total", runtime.totalMemory());
			memory.put("max", runtime.maxMemory());
		} catch (final Exception e) {
            Log.w(TAG, "getInfo failed to fully retrieve memory details", e);
		}
		return memory;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public Object getTotalMemory() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			return this.memoryInfo.totalMem;
		}
		else {
			return null;
		}
	}
	
	/**
	 * @return in kiloHertz.
	 * @throws SystemUtilsException
	 */
	public int getCPUFrequencyMax(int index) throws Exception {
		return readSystemFileAsInt("/sys/devices/system/cpu/cpu" + index + "/cpufreq/cpuinfo_max_freq");
	}
	
	private String readSystemFile(final String pSystemFile) {
		String content = "";
		InputStream in = null;
		try {
	      final Process process = new ProcessBuilder(new String[] { "/system/bin/cat", pSystemFile }).start();
	      in = process.getInputStream();
	      content = readFullyAndClose(in);
	    } catch (final Exception e) {
            Log.w(TAG, "getInfo unable to read system file");
	    }
		return content;
	}
	
	private int readSystemFileAsInt(final String pSystemFile) throws Exception {
		String content = readSystemFile(pSystemFile);
		if (content == "") {
			return 0;
		}
		return Integer.parseInt( content );
	}
	
	private String readFullyAndClose(final InputStream pInputStream) throws IOException {
		Scanner sc = null;
		try {
			final StringBuilder sb = new StringBuilder();
			sc = new Scanner(pInputStream);
			try {
			    while(sc.hasNextLine()) {
			      sb.append(sc.nextLine());
			    }
			    return sb.toString();
			} finally {
				sc.close();
				sc = null;
			}
		} finally {
			// paranoia is for the paranoid
			if (null == sc) {
				pInputStream.close();
			}
		}
	}
}
