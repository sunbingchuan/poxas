package com.bc.algorithm.poxas;

import static com.bc.algorithm.poxas.CommonMsg.*;

import java.util.concurrent.Semaphore;



// paxos test
public class DistributedNode implements Runnable {
	private int port = 0;
	private volatile long id = 0;
	private volatile String value = EMPTY;
	private Semaphore sp = new Semaphore(1);
	private boolean finished = false;
	private Acceptor acceptor = new Acceptor(this);
	private Proposer proposer = new Proposer(this);
	private String defaultValue = EMPTY;


	private DistributedNode() {}

	public DistributedNode(int port, String defaultValue) {
		this.port = port;
		this.defaultValue = defaultValue;
	}


	@Override
	public void run() {
		pool.execute(acceptor);
		// keeping on propose until the resoluttion is generated
		while (true) {
			try {
				sp.acquire();
				Long tmpid = generateProposeId();
				proposer.proposeFirst(id = tmpid);
				if (id != tmpid) {
					continue;
				}
				proposer.proposeSecond(id, value);

			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				sp.release();
			}
		}
	}



	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Acceptor getAcceptor() {
		return acceptor;
	}

	public void setAcceptor(Acceptor acceptor) {
		this.acceptor = acceptor;
	}

	public Proposer getProposer() {
		return proposer;
	}

	public void setProposer(Proposer proposer) {
		this.proposer = proposer;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public Semaphore getSp() {
		return sp;
	}

	public void setSp(Semaphore sp) {
		this.sp = sp;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}


}
