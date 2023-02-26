// Alumnos de la pareja: Alvaro de Rojas Maraver y Luis Alberto Lopez Alvarez.
// En esta clase de Java implementamos un buffer que almacena el valor del contador y
// mediante versiones obtenemos el valor de este y le sumamos uno en cada ejecucion
// guardando el nuevo valor en el buffer

package es.upm.dit.cnvr.lab1;

// Importamos las bibliotecas y librerias Java necesarias.
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.Random;

//Importamos las bibliotecas y librerias Zookeeper necesarias.
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper; 
import org.apache.zookeeper.data.Stat;


public class Contador implements Watcher{
	private static final int SESSION_TIMEOUT = 5000;
	

	private static String rootMembers = "/members";
	private static String aMember = "/member-";
	private String myId;
	private String lider;
	
	private boolean soyLider = false;
	private int counter = 0;
	
	private ByteBuffer buffer;
	private	Stat s = null;
	private byte[] b;
	
	// This is static. A list of zookeeper can be provided for decide where to connect.
	String[] hosts = {"127.0.0.1:2181", "127.0.0.1:2181", "127.0.0.1:2181"};

	private ZooKeeper zk;
	
	public Contador() {

		// Select a random zookeeper server
		Random rand = new Random();
		int i = rand.nextInt(hosts.length);
		
		// Create a session and wait until it is created.
		try {
			if (zk == null) {
				zk = new ZooKeeper(hosts[i], SESSION_TIMEOUT, cWatcher);
				try {
					wait();
				} catch (Exception e) {
					System.out.println("Error");
				}
			}
		} catch (Exception e) {
			System.out.println("Error");
		}

		// Add the process to the members in zookeeper
		if (zk != null) {
			// Create a folder for members and include this process/server
			try {
				// Create a folder, if it is not created
				String response = new String();
				Stat s = zk.exists(rootMembers, watcherMember); //this);
				if (s == null) {
					// Created the znode, if it is not created.
					response = zk.create(rootMembers, new byte[0], 
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					System.out.println(response);
				}
				buffer = ByteBuffer.allocate(4);
        		buffer.putInt(0);
        		
				// Create a znode for registering as member and get my id				
				myId = zk.create(rootMembers + aMember, buffer.array(), 
						Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
				myId = myId.replace(rootMembers + "/", "");

				List<String> list = zk.getChildren(rootMembers, watcherMember, s); //this, s);
				System.out.println("Created znode nember id:"+ myId );
				
				printListMembers(list);
				elegirLider(list);
			
				b = zk.getData("/members/" + lider, false, s);
				buffer = ByteBuffer.wrap(b);
				counter = buffer.getInt();
				counter+=1;
				buffer = ByteBuffer.allocate(4);	
        		buffer.putInt(counter);
        		s = zk.exists("/members/" + lider, false);
        		zk.setData("/members/" + lider, buffer.array(), s.getVersion());		
				System.out.println("Contador: " + counter);
				Thread.sleep(100000);
			
				b = zk.getData("/members/" + lider, false, s);
				buffer = ByteBuffer.wrap(b);
				counter = buffer.getInt();
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
				List<String> list = zk.getChildren(rootMembers,  watcherMember); //this);
				printListMembers(list);
				
				elegirLider(list);
			} catch (Exception e) {
				System.out.println("Exception: wacherMember");
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
			List<String> list = zk.getChildren(rootMembers, watcherMember); //this);
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
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			System.out.print(string + ", ");				
		}
		System.out.println();
	}
	
	private void elegirLider(List<String> list) {
		Collections.sort(list);
		lider = list.get(0);
		System.out.println("El lider es: " + lider);
		
		if(lider.equals(myId)) {
			soyLider = true;
		} else {
			soyLider = false;
		}
	}
	
	public static void main(String[] args) {
		Contador contador = new Contador();
	}
}