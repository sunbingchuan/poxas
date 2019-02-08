package com.bc.algorithm.poxas;

import static com.bc.algorithm.poxas.CommonMsg.ACCEPT;
import static com.bc.algorithm.poxas.CommonMsg.REFUSE;
import static com.bc.algorithm.poxas.EasyNio.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Acceptor implements Runnable, Callback {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private DistributedNode distributedNode;

	public Acceptor(DistributedNode distributedNode) {
		this.distributedNode = distributedNode;
	}

	public void accept() {
		server(distributedNode.getPort(), this);
	}

	@Override
	public void run() {
		accept();
	}

	@Override
	public Object execute(Object para) {
		String tmp = (String) para;
		String[] s = tmp.split(",");
		Long tmpid;
		logger.debug("acceptor {} receive req {}", distributedNode.getPort(), tmp);
		if (s.length < 2) {
			// if (distributedNode.getId() >= (tmpid = Long.parseLong(s[0]))) {
			// return REFUSE;
			// }
			return distributedNode.getId() + "," + distributedNode.getValue();
		} else {
			if (distributedNode.getId() > (tmpid = Long.parseLong(s[0]))) {
				if (StringUtils.equals(s[1], distributedNode.getValue())) {
					return ACCEPT;
				} else {
					return REFUSE;
				}
			} else {
				distributedNode.setId(tmpid);
				if (!StringUtils.equals(s[1], distributedNode.getValue())) {
					distributedNode.setValue(s[1]);
					if (distributedNode.getSp().availablePermits() == 0 && distributedNode.isFinished()) {
						distributedNode.setFinished(false);
						distributedNode.getSp().release();
						logger.debug("{} release value {}", distributedNode.getPort(), distributedNode.getValue());
					}
				}
				return ACCEPT;
			}
		}
	}

	public static void main(String[] args) {

	}
}
