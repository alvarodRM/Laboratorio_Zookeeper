package es.upm.dit.cnvr.lab1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

import javax.print.DocFlavor.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class Barrier implements Watcher {

	static ZooKeeper zk = null;

	private int size;
	private String name; 
	String root;
	int nWatchers;

	Integer mutexBarrier = -1;

	String userDirectory = "/home/alvaro.derojas.maraver/Desktop/lab1_FCON/Lab_Zookeeper";
	static Logger logger = Logger.getLogger(zkMember.class);
	// IF ERROR, SET THE CORRECT PATH OF A VALID log4j.properties 
	String log4jConfPath1 = "/home/alvaro.derojas.maraver/Desktop/lab1_FCON/Lab_Zookeeper/src/es/upm/dit/cnvr/lab1/log4j.properties";
	//String log4jConfPath = "/Users/aalonso/git/Lab_Zookeeper/Lab_Zookeeper/src/es/upm/dit/cnvr/lab1/log4j.properties";
	String log4jConfPath = "/home/alvaro.derojas.maraver/Desktop/lab1_FCON/Lab_Zookeeper/src/es/upm/dit/cnvr/lab1/log4j.properties";
	boolean config_log4j = false;

	/**
	 * Barrier constructor.
	 * 
	 * This method creates a session, if necessary, creates the zNode with the root parameter, 
	 * if necessary, and creates a child of the previous node. 
	 *
	 *
	 * @param address The address of a ZK server
	 * @param root The zNode root for managing the processes in the barrier
	 * @param size Number of processes for waiting
	 */

	public Barrier(String address, String root, int size) {
		if (config_log4j) {
			try {
				System.out.println(log4jConfPath1);
				// Configure Logger
				//BasicConfigurator.configure();
				PropertyConfigurator.configure(log4jConfPath1);
				logger.setLevel(Level.TRACE);//(Level.INFO);			
			} catch (Exception E){
				System.out.println("The path of log4j.properties is not correct");
			}
		}
		// Create a session and wait until it is created.
		// When is created, the watcher is notified
		if(zk == null){
			try {
				//System.out.println("Starting ZK:");
				zk = new ZooKeeper(address, 3000, this);

				//System.out.println("Finished starting ZK: " + zk);
			} catch (IOException e) {
				System.out.println(e.toString());
				zk = null;
			}
		}

		this.root = root;
		this.size = size;

		// Create a zNode root for managing the barrier, if it does not exist
		if (zk != null) {
			try {
				Stat s = zk.exists(root, false);
				if (s == null) {
					// Created the znode, if it is not created.
					zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
							CreateMode.PERSISTENT);
				}
			} catch (KeeperException e) {
				System.out.println("Keeper exception when instantiating queue: "
						+ e.toString());
			} catch (InterruptedException e) {
				System.out.println("Interrupted exception");
			}
		}

		// My node name
		try {
			//hostname
			name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString());
		} catch (UnknownHostException e) {
			System.out.println(e.toString());
		}
	}


	/**
	 * This method is invoked when the default watcher is fired.
	 * Its used to detect that the session has been finished
	 */
	public void process(WatchedEvent event) {
		nWatchers++;
		System.out.println("  Watcher event: " + event.toString() + ", " + nWatchers);
		//System.out.println("Process: " + event.getType());
		synchronized (mutexBarrier) {
			mutexBarrier.notify();
		}
	}


	/**
	 * This method is invoked when a process wants to enter in the barrier.
	 * It waits until the expected processes enter in the barrier
	 *
	 * @return Return the id of the node created
	 * @throws KeeperException Exception while the creation
	 * @throws InterruptedException Exception in the synchronized object
	 */

	public String enter() throws KeeperException, InterruptedException{
		String node = zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE,
				CreateMode.EPHEMERAL_SEQUENTIAL);
		System.out.println(node);
		//		zk.exists(root + "/" + name, this);
		if (zk.exists(node , this) == null) {
			return null;
		};

		while (true) {
			List<String> list = zk.getChildren(root, true);

			if (list.size() < size) {
				synchronized (mutexBarrier) {
					mutexBarrier.wait();
				}
			} else {
				return node;
			}
		}
	}

	/**
	 * This method is invoked when a process wants to leave from the barrier.
	 * Wait until the expected processes left the barrier
	 *
	 * @return Return true when finished the method.
	 * @throws KeeperException Exception while the creation
	 * @throws InterruptedException Exception in the synchronized object
	 */

	boolean leave(String node) throws KeeperException, InterruptedException{
		//zk.delete(root + "/" + name, 0);
		zk.delete(node, 0);
		while (true) {
			List<String> list = zk.getChildren(root, true);
			if (list.size() > 0) {
				synchronized (mutexBarrier) {
					mutexBarrier.wait();
				}
			} else {
				return true;
			}
		}
	}

	public static void main(String args[]) {
		//System.out.println(args.length);
		System.out.println(args[0] + " Entered barrier: " + args[1]);

		String node = "";

		// Create a barrier
		Barrier b = new Barrier(args[0], "/b1", new Integer(args[1]));
		try{
			// Enter in the barrier
			//boolean flag = b.enter();
			node = b.enter();
			System.out.println("Entered barrier: " + args[1]);
			if(node == null) System.out.println("Error when entering the barrier");
		} catch (KeeperException e){
			System.out.println("KeeperException in main, enter ");
		} catch (InterruptedException e){
			System.out.println("InterruptedException in main, enter ");
		}

		// Generate random integer
		Random rand = new Random();
		int r = rand.nextInt(100);
		// Loop for rand iterations
		for (int i = 0; i < r; i++) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.out.println("Exception in the sleep in main");
			}
		}
		try{
			// Leave the barrier
			b.leave(node);
		} catch (KeeperException e){
			System.out.println("KeeperException in main, leave ");
			//System.out.println(e.printStackTrace());
			System.out.println(e.toString());
		} catch (InterruptedException e){
			System.out.println("InterruptedException in main, leave ");
		}
		System.out.println("Left barrier");
	}
}