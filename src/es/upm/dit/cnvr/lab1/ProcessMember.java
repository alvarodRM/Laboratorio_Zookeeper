package es.upm.dit.cnvr.lab1;

import java.util.List;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;


public class ProcessMember extends Thread{

	private List<String> listMembersP = null;
	private int npMembers = 0;
	private String rootMembers = "/members";
	private ZooKeeper zk; 
	private Watcher memberWatcherP;
	private Integer mutex;

	public ProcessMember(ZooKeeper zk, Watcher memberWatcherP, Integer mutex) {
		this.zk = zk;
		this.memberWatcherP = memberWatcherP;
		this.mutex = mutex;
	}

	@Override
	public void run() {
		Stat s = null;
		while (true) {
			try {
				synchronized (mutex) {
					mutex.wait();
				}
				listMembersP = zk.getChildren(rootMembers, memberWatcherP, s); 
				npMembers ++;
				System.out.println("Current # of members: " + listMembersP.size());
			} catch (Exception e) {
				System.out.println("Unexpected Exception process member");
				break;
			}
		}		
	}
}