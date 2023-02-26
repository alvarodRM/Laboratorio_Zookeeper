package es.upm.dit.cnvr.lab1;

import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.util.Random;

// Import log4j classes.
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper; 
import org.apache.zookeeper.data.Stat;

/**
 * This is a simple application for detecting the correct processes using ZK. 
 * Several instances of this code can be created. Each of them detects the 
 * valid numbers.
 *
 * Two watchers are used:
 * - cwatcher: wait until the session is created. 
 * - watcherMember: notified when the number of members is updated

 * the method process has to be created for implement Watcher. However
 * this process should never be invoked, as the "this" watcher is used
 */
public class zkMember implements Watcher{

	private static final int SESSION_TIMEOUT = 5000;

	private static String rootMembers = "/members";
	private static String aMember = "/member-";
	private String myId;

	Integer mutexBarrier = -1;

	// This is static. A list of zookeeper can be provided for decide where to connect
	// This configured for a standalone ensemble. 
	String[] hosts = {"127.0.0.1:2181", "127.0.0.1:2181", "127.0.0.1:2181"};

	private ZooKeeper zk;

	String userDirectory = System.getProperty("user.dir");
	static Logger logger = Logger.getLogger(zkMember.class);
	// IF ERROR, SET THE CORRECT PATH OF A VALID log4j.properties 
	//String log4jConfPath = "/Users/aalonso/ownCloudUPM/home/docencia/DSCC/workspace/Lab_Zookeeper/src/es/upm/dit/cnvr/lab1/log4j.properties";
	//String log4jConfPath = "/Users/aalonso/git/Lab_Zookeeper/Lab_Zookeeper/src/es/upm/dit/cnvr/lab1/log4j.properties";
	String log4jConfPath = userDirectory + "/src/es/upm/dit/cnvr/lab1/log4j.properties";
	boolean config_log4j = false;
	
	/**
	 * This method creates a session, if necessary, creates the zNode member, 
	 * if necessary, and creates a child of the previous node. 
	 */
	public zkMember () {
		if (config_log4j) {
			try {
				System.out.println(log4jConfPath);
				// Configure Logger
				//BasicConfigurator.configure();
				PropertyConfigurator.configure(log4jConfPath);
				logger.setLevel(Level.TRACE);//(Level.INFO);			
			} catch (Exception E){
				System.out.println("The path of log4j.properties is not correct");
			}
		}

		// Select a random zookeeper server
		Random rand = new Random();
		int i = rand.nextInt(hosts.length);

		// Create a session and wait until it is created.
		// When is created, the watcher is notified
		try {
			if (zk == null) {
				zk = new ZooKeeper(hosts[i], SESSION_TIMEOUT, cWatcher);
				try {
					// Wait for creating the session. Use the object lock
					synchronized (mutexBarrier) {
						mutexBarrier.wait();
					}
					//zk.exists("/",false);
				} catch (Exception e) {
					System.out.println("Exception in the wait while creating the session");
				}
			}
		} catch (Exception e) {
			System.out.println("Exception while creating the session");
		}

		// Create a zNode "member", if it does not exist

		if (zk != null) {
			// Create a folder for members and include this process/server
			try {
				// Create a node with the name "member", if it is not created
				// Set a watcher
				String response = new String();
				Stat s = zk.exists(rootMembers, watcherMember);
				if (s == null) {
					// Created the znode, if it is not created.
					response = zk.create(rootMembers, new byte[0], 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					System.out.println(response);
				}

				// Create a znode for registering as member and get my id
				myId = zk.create(rootMembers + aMember, new byte[0], 
						Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

				myId = myId.replace(rootMembers + "/", "");

				// Get the children of a node and set a watcer
				List<String> list = zk.getChildren(rootMembers, watcherMember, s); //this, s);
				System.out.println("Created znode nember id:"+ myId );
				printListMembers(list);
			} catch (KeeperException e) {
				System.out.println("The session with Zookeeper failes. Closing");
				return;
			} catch (InterruptedException e) {
				System.out.println("InterruptedException raised");
			}

		}
	}

	/**
	 * This variable creates a new watcher. It is fired when the session 
	 * is created
	 */
	private Watcher cWatcher = new Watcher() {
		public void process (WatchedEvent e) {
			System.out.println("Created session");
			System.out.println(e.toString());
			synchronized (mutexBarrier) {
				mutexBarrier.notify();
			}
		}
	};

	/**
	 * This variable creates a new watcher. It is fired when a child of "member"
	 * is created or deleted.
	 */
	private Watcher  watcherMember = new Watcher() {
		/**
		 *  This method is executed whenever the watcher is fired
		 */
		public void process(WatchedEvent event) {
			System.out.println("------------------Watcher Member------------------\n");		
			try {
				System.out.println("        Update!!");
				//System.out.println("!!!!!!" + event.toString());
				// Get children and set the watcher
				List<String> list = zk.getChildren(rootMembers,  watcherMember); 
				printListMembers(list);
			} catch (Exception e) {
				System.out.println("Exception: watcherMember");
			}
		}
	};

	/**
	 *  This method is executed whenever the watcher is fired.
	 *  This process must be created
	 */
	@Override
	public void process(WatchedEvent event) {
		try {
			System.out.println("Unexpected invocated this method. Process of the object");
			List<String> list = zk.getChildren(rootMembers, watcherMember);
			printListMembers(list);
		} catch (Exception e) {
			System.out.println("Unexpected exception. Process of the object");
		}
	}

	/**
	 * Print a list
	 * @param list The list to be printed
	 */
	private void printListMembers (List<String> list) {
		System.out.println("Remaining # members:" + list.size());
		for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			System.out.print(string + ", ");				
		}
		System.out.println();

	}

	public static void main(String[] args) {
		zkMember zk = new zkMember();

		try {
			Thread.sleep(300000); 			
		} catch (Exception e) {
			System.out.println("Exception in the sleep in main");
		}
	}
}
