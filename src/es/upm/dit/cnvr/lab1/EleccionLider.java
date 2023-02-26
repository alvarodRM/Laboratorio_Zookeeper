// Alumnos de la pareja: Alvaro de Rojas Maraver y Luis Alberto Lopez Alvarez.
// En esta clase de Java implementamos el metodo elegirLider(), que recibe una 
// lista de Strings y la ordena, eligiendo como lider al primer elemento de la
// lista ordenada

package es.upm.dit.cnvr.lab1;

//Importamos las bibliotecas y librerias Java necesarias.
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Collections;

//Importamos las bibliotecas y librerias Zookeeper necesarias.
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper; 
import org.apache.zookeeper.data.Stat;


public class EleccionLider implements Watcher{

	private static final int SESSION_TIMEOUT = 5000;

	private static String rootMembers = "/members";
	private static String aMember = "/member-";
	private String myId;
	private String lider;

	// This is static. A list of zookeeper can be provided for decide where to connect
	// This configured for a standalone ensemble. 
	String[] hosts = {"127.0.0.1:2181", "127.0.0.1:2181", "127.0.0.1:2181"};

	private ZooKeeper zk;


	public EleccionLider () {

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
					wait();
					//zk.exists("/",false);
				} catch (Exception e) {
					System.out.println("Exception in the wait while creating the session");
				}
			}
		} catch (Exception e) {
			System.out.println("Exception while creating the session");
		}

		// Add a zNode "member", if it does not exist
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
				elegirLider(list);
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
			notify();
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
				elegirLider(list);
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
			// elegirLider(list);
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
	
	private void elegirLider (List<String> list) {
		Collections.sort(list);
		lider = list.get(0);
		System.out.println("El lider ahora es: " + lider); 
	}

	public static void main(String[] args) {
		EleccionLider eleccionlider = new EleccionLider();
		try {
			Thread.sleep(300000); 			
		} catch (Exception e) {
			System.out.println("Exception in the sleep in main");
		}
	}
}
