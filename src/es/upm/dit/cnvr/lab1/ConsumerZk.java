package es.upm.dit.cnvr.lab1;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import java.nio.ByteBuffer;

public class ConsumerZk implements Watcher{

	private ZooKeeper zk = null;
	private String rootProducts = "/products"; 
	private List<String> listProducts = null;
	private int nProducts = 0;
	private int nProductsMax = 0;	
	private int nProductsWatcher = 0;
	private int id = 0;
	private Integer mutex        = -1;
	private static final int SESSION_TIMEOUT = 2000;

	public ConsumerZk(int nProductsMax, int id){

		this.nProductsMax = nProductsMax;
		this.id = id;

		// This is static. A list of zookeeper can be provided for decide where to connect
		String[] hosts = {"127.0.0.1:2181", "127.0.0.1:2181", "127.0.0.1:2181"};

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
			// Create a folder for products and include this process/server
			try {
				// Create the /products znode
				// Create a folder, if it is not created
				Stat s = zk.exists(rootProducts, false);
				if (s == null) {
					// Created the znode, if it is not created.
					zk.create(rootProducts, new byte[0],
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}

//				listMembers = zk.getChildren(rootMembers,  memberWatcher, s);

			} catch (KeeperException e) {
				System.out.println("The session with Zookeeper failes. Closing");
				return;
			} catch (InterruptedException e) {
				System.out.println("InterruptedException raised");
			}

		}
	}

	// Wait for finishing the connection.
	@Override
	public void process(WatchedEvent event) {
		Stat s = null;

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
	
	Watcher productWatcher = new Watcher() {
		public void process(WatchedEvent event) { 

			Stat s = null;

			System.out.println("------------------Watcher Product ------------------");
			System.out.println("Member: " + event.getType() + ", " + event.getPath());
			try {
				if (event.getPath().equals(rootProducts)) {
				listProducts = zk.getChildren(rootProducts, false, s);
						nProductsWatcher ++;
						System.out.println("# of Members watchers: " + nProductsWatcher);
						printListMembers(listProducts);
						synchronized (mutex) {
							mutex.notify();
						}
				} else {
					System.out.println("Product: Received a watcher with a path not expected");
				}

			} catch (Exception e) {
				System.out.println("Unexpected Exception process");
			}
		}
	};
	
	private void consume() {

		Stat s = null;
		String path = null;
		int data = -1;
		

		// while the veces
		while (nProducts < nProductsMax) {
			try {
				listProducts = zk.getChildren(rootProducts, false, s); 
			} catch (Exception e) {
				System.out.println("Unexpected Exception process barrier");
				break;
			}
			
			if (listProducts.size() > 0) {
				try {
//					System.out.println(listProducts.get(0));
					path = rootProducts+"/"+listProducts.get(0);
//					System.out.println(path);
					byte[] b = zk.getData(path, false, s);
					s = zk.exists(path, false);
					//System.out.println(s.getVersion());
					zk.delete(path, s.getVersion());
					
					// Generate random delay
					Random rand = new Random();
					int r = rand.nextInt(10);
					// Loop for rand iterations
					for (int j = 0; j < r; j++) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {

						}
					}
					
                    ByteBuffer buffer = ByteBuffer.wrap(b);
                    data = buffer.getInt();
                    nProducts++;
                    System.out.println("++++ Consume. Data: " + data + "; Path: " + path + "; Number of products: " + nProducts);
				} catch (Exception e) {
					// The exception due to a race while getting the list of children, get data and delete. Another
					// consumer may have deleted a child while the previous access. Then, the exception is simply
					// implies that the data has not been produced.
					System.out.println("Exception when accessing the data");
					//System.err.println(e);
					//e.printStackTrace();
					//break;
				}
			} else {
				try {
					zk.getChildren(rootProducts, productWatcher, s);
					synchronized(mutex) {
						mutex.wait();
					}
				} catch (Exception e) {
					System.out.println("Unexpected Exception process barrier");
					break;
				}
			}			
		}
		// get list. 
		// si hay proceso y borro
		// si no hay, armo un watcher y espero. 

	}

	private void printListMembers (List<String> list) {
		System.out.println("Remaining # members:" + list.size());
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			System.out.print(string + ", ");				
		}
		System.out.println();

	}
	
	public static void main(String[] args) {
		// Read 10 items
		int nProducts = 20;
		Integer id;
		if (args.length == 0) {
			id = 0;
		} else {
			id = Integer.parseInt(args[0]);
		}
		ConsumerZk consumer = new ConsumerZk(nProducts, id);
		consumer.consume();
	}

}
