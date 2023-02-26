package es.upm.dit.cnvr.lab1;

import java.util.List;
import java.util.Random;
//import java.util.Scanner;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class ExploreZk implements Watcher{

	private static ZooKeeper zk = null;
	private static String rootMembers = "/members";
	private static String rootBarrier = "/b1";
	//private static String aMember = "/member-";
	private static int nMembers  = 0;
	private static int nBarriers = 0;
	//private static List<String> listBarriers = null;
	private static List<String> listMembers  = null;

	private static Integer mutex        = -1;
	private static Integer mutexBarrier = -2;
	private static Integer mutexMember  = -3;
	private static final int SESSION_TIMEOUT = 5000;
	//private String myId = null;

	public ExploreZk() { 

	}

	public void configure() {	
		// This is static. A list of zookeeper can be provided for decide where to connect
		String[] hosts = {"127.0.0.1:2181", "127.0.0.1:2181"};

		// Select a random zookeeper server
		Random rand = new Random();
		int i = rand.nextInt(hosts.length);

		// Create the session
		// Create a session and wait until it is created.
		// When is created, the watcher is notified
		try {
			if (zk == null) {
				zk = new ZooKeeper(hosts[i], SESSION_TIMEOUT, this);
				// We initialize the mutex Integer just after creating ZK.
				try {
					// Wait for creating the session. Use the object lock
					synchronized(mutex) {
						mutex.wait();
					}
					//zk.exists("/", false);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		} catch (Exception e) {
			System.out.println("Exception in constructor");
		}

		// Add the process to the members in zookeeper

		if (zk != null) {
			// Create a folder for members and include this process/server
			try {
				// Create the /members znode
				// Create a folder, if it is not created
				Stat s = zk.exists(rootMembers, false);
				if (s == null) {
					// Created the znode, if it is not created.
					zk.create(rootMembers, new byte[0],
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}

				// Create a znode for registering as member and get my id
				//myId = zk.create(rootMembers + aMember, new byte[0],
				//		Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
				//myId = myId.replace(rootMembers + "/", "");
				// false. Debe esperar a arrancar el barrir
				listMembers = zk.getChildren(rootMembers,  memberWatcher, s);
				//System.out.println("Created znode nember id:"+ myId );


			} catch (KeeperException e) {
				System.out.println("The session with Zookeeper failes. Closing");
				return;
			} catch (InterruptedException e) {
				System.out.println("InterruptedException raised");
			}

			// Create the /b1 znode
			if (zk != null) {
				try {
					Stat s = zk.exists(rootBarrier, false);
					if (s == null) {
//						zk.create(rootBarrier, new byte[0], Ids.OPEN_ACL_UNSAFE,
//								CreateMode.PERSISTENT);
						zk.create(rootBarrier, new byte[0],
								Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
					}

					
					//myId = myId.replace(rootMembers + "/", "");
					// false. Debe esperar a arrancar el barrier
					listMembers = zk.getChildren(rootBarrier,  barrierWatcher, s);

				} catch (KeeperException e) {
					System.out.println("Keeper exception when instantiating queue: "
							+ e.toString());
				} catch (InterruptedException e) {
					System.out.println("Interrupted exception");
				}
			}
		}

		// Create threads
		ProcessMember pm = new ProcessMember(zk, memberWatcher, mutexMember);
		pm.start();
		ProcessBarrier bm = new ProcessBarrier(zk, barrierWatcher, mutexBarrier);
		bm.start();
	}

	// Assigned to members
	@Override
	public void process(WatchedEvent event) {
		//Stat s = null;

		System.out.println("------------------Watcher PROCESS ------------------");
		System.out.println("Member: " + event.getType() + ", " + event.getPath());
		try {
			if (event.getPath() == null) {			
				//if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
				System.out.println("SyncConnected");
				synchronized (mutex) {
					mutex.notify();
				}
			}
			System.out.println("-----------------------------------------------");
		} catch (Exception e) {
			System.out.println("Unexpected Exception process");
		}
	}


	Watcher memberWatcher = new Watcher() {
		public void process(WatchedEvent event) { 

			//Stat s = null;

			System.out.println("------------------Watcher MEMBER ------------------");
			System.out.println("Member: " + event.getType() + ", " + event.getPath());
			try {
				if (event.getPath() == null) {			
					//if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
					System.out.println("SyncConnected");
					synchronized (mutex) {
						mutex.notify();
					}
				}
				else if (event.getPath().equals(rootMembers)) {
					//listMembers = zk.getChildren(rootMembers, this, s);
					synchronized (mutexMember) {
						nMembers ++;
						System.out.println("# of Members watchers: " + nMembers);
						mutexMember.notify();
					}
				}
				else if (event.getPath().equals(rootBarrier)) {
					//listBarriers = zk.getChildren(rootBarrier, this.barrierWatcher, s); 
					synchronized (mutexBarrier) {
						nBarriers ++;
						System.out.println("Unexpeted to handle this watcher. MW NBarriers: " + nBarriers);
						mutexBarrier.notify();
					}
				} else {
					System.out.println("Member: Received a watcher with a path not expected");
				}

				//System.out.println("-----------------------------------------------");
			} catch (Exception e) {
				System.out.println("Unexpected Exception process");
			}
		}
	};

	Watcher barrierWatcher = new Watcher() {
		public void process(WatchedEvent event) {
			//Stat s = null;

			System.out.println("------------------Watcher BARRIER ------------------");
			System.out.println("Barrier: " + event.getType() + ", " + event.getPath());
			try {
				if (event.getPath() == null) {			
					//if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
					System.out.println("SyncConnected");
					synchronized (mutex) {
						mutex.notify();
					}
				}
				else if (event.getPath().equals(rootMembers)) {
					//listMembers = zk.getChildren(rootMembers, process, s);
					//&synchronized (mutexMember) {
					synchronized (mutexMember) {
						nMembers ++;
						System.out.println("Unexpeted to handle this watcher. BW Members: " + nMembers);
						mutexMember.notify();
					}
				}
				else if (event.getPath().equals(rootBarrier)) {
					//listBarriers = zk.getChildren(rootBarrier, this.barrierWatcher, s); 
					synchronized (mutexBarrier) {
						nBarriers ++;
						System.out.println("# of Barriers watchers: " + nBarriers);
						mutexBarrier.notify();
					}
				} else {
					System.out.println("Barrier: Received a watcher with a path not expected");
				}
				//System.out.println("-----------------------------------------------");
			} catch (Exception e) {
				System.out.println("Unexpected Exception process");
			}
		}
	};

/*	public void setBarrier() {
		try {
			Stat s = null;
			zk.getChildren(rootMembers, false, s);
			zk.getChildren(rootBarrier, barrierWatcher, s);
		} catch (Exception e) {
			System.out.println("Unexpected Exception setBarrier");
		}
	}

	public void setMember() {
		try {
			Stat s = null;
			zk.getChildren(rootBarrier, false, s);			
			zk.getChildren(rootMembers, memberWatcher, s);
		} catch (Exception e) {
			System.out.println("Unexpected Exception setMember");
		}
	}
*/
	public static void main(String[] args) {
		ExploreZk eZk = new ExploreZk();
		eZk.configure();		
	}
}


