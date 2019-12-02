package nkolbow.debug;

public class Debug {
	
	private static int DEBUG_LEVEL = 3;
	
	/**
	 * Standardizes debug messages
	 * 
	 * @param msg - message to be sent
	 * @param level - debug level
	 * 				  3: fatal messages
	 * 				  2: important warnings
	 * 				  1: normal dev debug messages
	 * 			      0: basic logs (for user to see)
	 *                
	 */
	public static void log(String msg, int level) {
		if(DEBUG_LEVEL >= level) {
			switch(level) {
			case 0:
				System.out.println("[INFO] " + msg);
				break;
			case 1:
				System.out.println("[DEBUG] " + msg);
				break;
			case 2:
				System.out.println("[WARNING] " + msg);
				break;
			case 3:
				System.out.println("[ERROR] " + msg);
				break;
			default:
				break;
			}
		}
	}
	
}
