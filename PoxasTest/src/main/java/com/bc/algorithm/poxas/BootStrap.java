package com.bc.algorithm.poxas;

import static com.bc.algorithm.poxas.CommonMsg.*;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootStrap {
	public static Logger logger = LoggerFactory.getLogger(BootStrap.class);

	public static void main(String[] args) throws InterruptedException {
		List<DistributedNode> lt = new ArrayList<DistributedNode>();
		for (int i = 0; i < list.length; i++) {
			DistributedNode node = new DistributedNode(list[i], msg[i % msg.length]);
			lt.add(node);
			pool.execute(node);
		}
		//show the result 
		while (true) {
			Thread.sleep(10000);
			StringBuffer sb = new StringBuffer();
			sb.append("\r\n------------------------------------------------------------\r\n");
			for (DistributedNode distributedNode : lt) {
				sb.append(distributedNode.getPort() + ":" + distributedNode.getId() + ":" + distributedNode.getValue()
						+ "\r\n");
			}
			sb.append("------------------------------------------------------------");
			logger.debug(sb.toString());
		}

	}
}
