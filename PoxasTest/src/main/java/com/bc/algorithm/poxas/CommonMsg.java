package com.bc.algorithm.poxas;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommonMsg {
	static final String EMPTY = "";
	static final String ACCEPT = "accept";
	static final String REFUSE = "refuse";
	static final int DEFAULT_INITIAL_CAPACITY = 1 << 8;
	static ExecutorService pool = Executors.newFixedThreadPool(DEFAULT_INITIAL_CAPACITY);
	static int[] list = null;
	/*
	 * {10000, 10001, 10002, 10003, 10004, 10005, 10006, 10007, 10008, 10009, 10010, 10011, 10012,
	 * 10013, 10014, 10015, 10016, 10017, 10018, 10019, 10020};
	 */

	static {
		list = new int[50];
		for (int i = 0; i < list.length; i++) {
			list[i] = 10000 + i;
		}
	}

	public static String[] msg = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"};

	public static long generateProposeId() {
		return System.nanoTime();
	}
}
