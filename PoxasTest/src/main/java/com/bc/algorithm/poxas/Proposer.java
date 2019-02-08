package com.bc.algorithm.poxas;

import static com.bc.algorithm.poxas.CommonMsg.*;
import static com.bc.algorithm.poxas.EasyNio.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Proposer {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private DistributedNode distributedNode;


	public Proposer(DistributedNode distributedNode) {
		this.distributedNode = distributedNode;
	}

	// propose request
	public void proposeFirst(Long id) {
		Map<String, Integer> cal = new HashMap<>();
		for (int port : list) {
			if (port == distributedNode.getPort()) {
				continue;
			}
			client(port, id + ",", new Callback() {
				@Override
				public Object execute(Object para) {
					logger.debug("{} proposeFirst return {}", distributedNode.getPort(), para);
					String tmp;
					if (StringUtils.isNotEmpty(tmp = (String) para) && tmp.indexOf(',') > 0) {
						String[] ar = tmp.split(",");
						if (ar.length > 1) {
							cal.put(ar[1], cal.get(ar[1]) == null ? 1 : (cal.get(ar[1]) + 1));
						}
					}
					return null;
				}
			});
		}
		Iterator<String> it = cal.keySet().iterator();
		String value = null;
		int max = 0;
		while (it.hasNext()) {
			String tmp = it.next();
			if (max < cal.get(tmp)) {
				value = tmp;
				max = cal.get(tmp);
			}
		}
		logger.debug("{} caculate Max num {},max value {}", new Object[] {distributedNode.getPort(), max, value});
		if (distributedNode.getId() == id) {
			if (StringUtils.isNotEmpty(value)) {
				distributedNode.setValue(value);
			} else {
				distributedNode.setValue(distributedNode.getDefaultValue());
			}
		}

	}

	// accept request
	public void proposeSecond(Long id, String value) {
		Map<String, Integer> cal = new HashMap<>();
		cal.put("con", 0);
		for (int port : list) {
			if (port == distributedNode.getPort()) {
				continue;
			}
			client(port, id + "," + value, new Callback() {
				@Override
				public Object execute(Object para) {
					logger.debug("{} proposeSecond return {}", distributedNode.getPort(), para);
					if (ACCEPT.equals(para)) {
						cal.put("con", cal.get("con") + 1);
					}
					return null;
				}
			});

		}
		int num = cal.get("con");
		logger.debug("{} accept request return {}", distributedNode.getPort(), num);
		if (StringUtils.equals(value, distributedNode.getValue()) && num > list.length / 2.0) {
			try {
				distributedNode.setFinished(true);
				distributedNode.getSp().acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}



}
