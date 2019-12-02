package nkolbow.board;

import java.util.ArrayList;

import nkolbow.board.minions.Deathrattle;
import nkolbow.board.minions.Effect;
import nkolbow.board.minions.Line;
import nkolbow.board.minions.Minion;
import nkolbow.board.minions.Minion.Tribe;
import nkolbow.debug.Debug;

public class Board {
	
	private Line enemies;
	private Line friends;
	public Line getEnemies() { return enemies; }
	public Line getFriends() { return friends; }
	
	private final int friendTier;
	private final int enemyTier;
	
	public Board(int friendTier, int enemyTier) {
		friends = new Line();
		enemies = new Line();
		
		this.friendTier = friendTier;
		this.enemyTier = enemyTier;
	}
	
	private static int totalWins = 0;
	private static int totalLosses = 0;
	private static int totalTies = 0;
	private static int maxDealt = 0;
	private static int maxTaken = 0;
	private static int totalDealt = 0;
	private static int totalTaken = 0;
	private static Board maxTakenBoard;
	private static Board maxDealtBoard;
	
	public static void battle(Board board) {
		totalWins = 0;
		totalLosses = 0;
		totalTies = 0;
		maxDealt = 0;
		maxTaken = 0;
		totalDealt = 0;
		totalTaken = 0;
		
		
		
		if(board.enemies.size() > board.friends.size()) {
			Debug.log("Only the enemy can attack first.", 0);
			battleRecur(board, false);
		} else if(board.friends.size() > board.enemies.size()) {
			Debug.log("Only we can attack first.", 0);
			battleRecur(board, true);
		} else {
			Debug.log("Both sides have the chance to attack first.", 0);
			Debug.log("======== FRIENDLY ATTACKING FIRST ========", 1);
			battleRecur(board, true);
			Debug.log("\n\n======== ENEMY ATTACKING FIRST ========", 1);
			battleRecur(board, false);
		}
		
		System.out.println("\nBATTLE STATS\n" + "=================================================\n"
							+ "Total possibilities: \t" + (totalTies + totalWins + totalLosses)
							+ "\nTotal wins: \t\t" + totalWins + "\t%Chance: ~" + String.format("%.2f", (((double)totalWins*100)/(double)(totalTies+totalWins+totalLosses))) + "%"
							+ "\nTotal losses: \t\t" + totalLosses + "\t%Chance: ~" + String.format("%.2f", (((double)totalLosses*100)/(double)(totalTies+totalWins+totalLosses))) + "%"
							+ "\nTotal ties: \t\t" + totalTies + "\t%Chance: ~" + String.format("%.2f", (((double)totalTies*100)/(double)(totalTies+totalWins+totalLosses))) + "%"
							+ "\n\nAverage dealt on wins: " + String.format("%.2f", ((double)totalDealt/(double)totalWins)) + "\t\tMax dealt: " + maxDealt
							+ "\nOverall average damage: " + String.format("%.2f", ((double)(totalTaken + totalDealt))/(totalWins + totalLosses + totalTies))
							+ ((maxDealtBoard == null) ? "" : "\n" + maxDealtBoard.toString())
							+ "\nAverage taken on losses: " + String.format("%.2f", (double)totalTaken/(double)totalLosses) + "\t\tMax taken: " + maxTaken
							+ ((maxTakenBoard == null) ? "" : "\n" + maxTakenBoard.toString() + "\n")
							+ "=================================================");
	}
	
	private static void battleRecur(Board mainBoard, boolean friendAttack) {
		if(mainBoard.enemies.isEmpty()) {
			if(mainBoard.friends.isEmpty()) {
				totalTies++;
				return;
			} else {
				totalWins++;
				if(mainBoard.friends.getDamage() + mainBoard.friendTier > maxDealt) {
					maxDealt = mainBoard.friends.getDamage() + mainBoard.friendTier;
					maxDealtBoard = mainBoard;
				}
				
				totalDealt += mainBoard.friends.getDamage() + mainBoard.friendTier;
				return;
			}
		} else if(mainBoard.friends.isEmpty()) {
			totalLosses++;
			if(mainBoard.enemies.getDamage() + mainBoard.enemyTier > maxTaken) {
				maxTaken = mainBoard.enemies.getDamage() + mainBoard.enemyTier;
				maxTakenBoard = mainBoard;
			}
			
			totalTaken += mainBoard.enemies.getDamage() + mainBoard.enemyTier;
			return;
		}
		
		ArrayList<Board> newBoards = new ArrayList<Board>();
		if(friendAttack) {
			
System.out.println("Board before:");
mainBoard.printBoard();
System.out.println(friendAttack);

			newBoards.addAll(mainBoard.friends.attack(mainBoard, friendAttack));

System.out.println("Outcome boards (" + newBoards.size() + "):");
for(Board b : newBoards)
	b.printBoard();

System.out.println("\n\n\n");
		
		} else {

System.out.println("Board before:");
mainBoard.printBoard();
System.out.println(friendAttack);

			newBoards.addAll(mainBoard.enemies.attack(mainBoard, friendAttack));

System.out.println("Outcome boards (" + newBoards.size() + "):");
for(Board b : newBoards)
	b.printBoard();

System.out.println("\n\n\n");
		}
		
//		Debug.log("Total board states found: " + newBoards.size(), 1);
//		for(Board b : newBoards)
//			b.printBoard();
		
		for(Board b : newBoards) {
			battleRecur(b, !friendAttack);
		}
	}
	
	/**
	 * 
	 * @param minion -  String code for the minion, as below:
	 * 					<name>.<tribe>.<attack>.<health>.<stars>;pdct;<deathrattles>;<effects>
	 * 					pdc optional, only appear if the minion has poisonous, divine shield
	 * 					or cleave respectively
	 * @param isFriend - Which board the minion goes on
	 * @param position - Position the minion is spawned into (1 - 7, or Integer.MAX_VALUE,
	 * 					 Integer.MAX_VALUE means add to the end)
	 * @throws FullBoardException - If the board that you're trying to add to is already full
	 */
	public void addMinion(String minion, boolean isFriend, int position) throws FullBoardException {
		if((isFriend) ? friends.size() >= 7 : enemies.size() >= 7) {
			throw new FullBoardException();
		} else {
			String name = minion.substring(0, minion.indexOf('.'));
			minion = minion.substring(minion.indexOf('.') + 1);
			Tribe tribe = readTribe(minion.substring(0, minion.indexOf('.')));
			minion = minion.substring(minion.indexOf('.') + 1);
			int attack = Integer.parseInt(minion.substring(0, minion.indexOf('.')));
			minion = minion.substring(minion.indexOf('.') + 1);
			int health = Integer.parseInt(minion.substring(0, minion.indexOf('.')));
			minion = minion.substring(minion.indexOf('.') + 1);
			int stars = Integer.parseInt(minion.substring(0, minion.indexOf(';')));
			minion = minion.substring(minion.indexOf(';') + 1);
			String attributes = minion.substring(0, minion.indexOf(';'));
			boolean divine = attributes.contains("d");
			boolean poison = attributes.contains("p");
			boolean cleave = attributes.contains("c");
			boolean taunt = attributes.contains("t");
			minion = minion.substring(minion.indexOf(';') + 1);
			ArrayList<Deathrattle> deathrattles = readDeathrattles(minion.substring(0, minion.indexOf(';')));
			minion = minion.substring(minion.indexOf(';') + 1);
			Effect effect = readEffects(minion);
			
			Minion toAdd = new Minion(name, tribe, (isFriend) ? friends : enemies, attack, health, health, stars, this, effect,
										divine, poison, cleave, taunt, deathrattles);
			
			if(isFriend) {
				if(position == Integer.MAX_VALUE)
					friends.addLast(toAdd);
				else
					friends.add(position, toAdd);
			} else {
				if(position == Integer.MAX_VALUE)
					enemies.addLast(toAdd);
				else
					enemies.add(position, toAdd);
			}
		}
	}
	
	private Effect readEffects(String str) {
		switch(str) {
		case "a":
			return Effect.Dire_Wolf_Alpha;
		case "A":
			return Effect.Gold_Dire_Wolf_Alpha;
		case "b":
			return Effect.Murloc_Warleader;
		case "B":
			return Effect.Gold_Murloc_Warleader;
		case "c":
			return Effect.Old_Murk_Eye;
		case "C":
			return Effect.Gold_Old_Murk_Eye;
		case "d":
			return Effect.Cobalt_Guardian;
		case "D":
			return Effect.Gold_Cobalt_Guardian;
		case "e":
			return Effect.Imp_Gang_Boss;
		case "E":
			return Effect.Gold_Imp_Gang_Boss;
		case "f":
			return Effect.Khadgar;
		case "F":
			return Effect.Gold_Khadgar;
		case "g":
			return Effect.Pack_Leader;
		case "G":
			return Effect.Gold_Pack_Leader;
		case "h":
			return Effect.Phalanx_Commander;
		case "H":
			return Effect.Gold_Phalanx_Commander;
		case "i":
			return Effect.Soul_Juggler;
		case "I":
			return Effect.Gold_Soul_Juggler;
		case "j":
			return Effect.Bolvar_Fireblood;
		case "J":
			return Effect.Gold_Bolvar_Fireblood;
		case "k":
			return Effect.Festeroot_Hulk;
		case "K":
			return Effect.Gold_Festeroot_Hulk;
		case "l":
			return Effect.Security_Rover;
		case "L":
			return Effect.Gold_Security_Rover;
		case "m":
			return Effect.Siegebreaker;
		case "M":
			return Effect.Gold_Siegebreaker;
		case "n":
			return Effect.Baron_Rivendare;
		case "N":
			return Effect.Gold_Baron_Rivendare;
		case "o":
			return Effect.Ironhide_Direhorn;
		case "O":
			return Effect.Gold_Ironhide_Direhorn;
		case "p":
			return Effect.Junkbot;
		case "P":
			return Effect.Gold_Junkbot;
		case "q":
			return Effect.MalGanis;
		case "Q":
			return Effect.Gold_MalGanis;
		case "r":
			return Effect.The_Boogeymonster;
		case "R":
			return Effect.Gold_The_Boogeymonster;
		case "s":
			return Effect.Mama_Bear;
		case "S":
			return Effect.Gold_Mama_Bear;
		case "t":
			return Effect.Zapp_Slywick;
		case "T":
			return Effect.Gold_Zapp_Slywick;
		default:
			return Effect.None;
		}
	}
	
	private Tribe readTribe(String str) {
		switch(str) {
		case "b":
			return Tribe.Beast;
		case "m":
			return Tribe.Murloc;
		case "M":
			return Tribe.Mech;
		case "D":
			return Tribe.Dragon;
		case "d":
			return Tribe.Demon;
		case "a":
			return Tribe.All;
		default:
			return Tribe.None;
			
		}
	}
	
	private ArrayList<Deathrattle> readDeathrattles(String str) {
		ArrayList<Deathrattle> toRet = new ArrayList<Deathrattle>();
		
		for(int i = 0; i < str.length(); i++) {
			switch(str.charAt(i)) {
			case 'a':
				toRet.add(Deathrattle.Mecharoo);
				break;
			case 'A':
				toRet.add(Deathrattle.Gold_Mecharoo);
				break;
			case 'b':
				toRet.add(Deathrattle.Selfless_Hero);
				break;
			case 'B':
				toRet.add(Deathrattle.Gold_Selfless_Hero);
				break;
			case 'c':
				toRet.add(Deathrattle.Harvest_Golem);
				break;
			case 'C':
				toRet.add(Deathrattle.Gold_Harvest_Golem);
				break;
			case 'd':
				toRet.add(Deathrattle.Kaboom_Bot);
				break;
			case 'D':
				toRet.add(Deathrattle.Gold_Kaboom_Bot);
				break;
			case 'e':
				toRet.add(Deathrattle.Kindly_Grandmother);
				break;
			case 'E':
				toRet.add(Deathrattle.Gold_Kindly_Grandmother);
				break;
			case 'f':
				toRet.add(Deathrattle.Mounted_Raptor);
				break;
			case 'F':
				toRet.add(Deathrattle.Gold_Mounted_Raptor);
				break;
			case 'g':
				toRet.add(Deathrattle.Rat_Pack);
				break;
			case 'G':
				toRet.add(Deathrattle.Gold_Rat_Pack);
				break;
			case 'h':
				toRet.add(Deathrattle.Spawn_of_NZoth);
				break;
			case 'H':
				toRet.add(Deathrattle.Gold_Spawn_of_NZoth);
				break;
			case 'i':
				toRet.add(Deathrattle.Infested_Wolf);
				break;
			case 'I':
				toRet.add(Deathrattle.Gold_Infested_Wolf);
				break;
			case 'j':
				toRet.add(Deathrattle.Piloted_Shredder);
				break;
			case 'J':
				toRet.add(Deathrattle.Gold_Piloted_Shredder);
				break;
			case 'k':
				toRet.add(Deathrattle.Replicating_Menace);
				break;
			case 'K':
				toRet.add(Deathrattle.Gold_Replicating_Menace);
				break;
			case 'l':
				toRet.add(Deathrattle.Tortollan_Shellraiser);
				break;
			case 'L':
				toRet.add(Deathrattle.Gold_Tortollan_Shellraiser);
				break;
			case 'm':
				toRet.add(Deathrattle.Piloted_Sky_Golem);
				break;
			case 'M':
				toRet.add(Deathrattle.Gold_Piloted_Sky_Golem);
				break;
			case 'n':
				toRet.add(Deathrattle.The_Beast);
				break;
			case 'N':
				toRet.add(Deathrattle.Gold_The_Beast);
				break;
			case 'o':
				toRet.add(Deathrattle.Goldrinn_the_Great_Wolf);
				break;
			case 'O':
				toRet.add(Deathrattle.Gold_Goldrinn_the_Great_Wolf);
				break;
			case 'p':
				toRet.add(Deathrattle.Mechano_egg);
				break;
			case 'P':
				toRet.add(Deathrattle.Gold_Mechano_egg);
				break;
			case 'q':
				toRet.add(Deathrattle.Sated_Threshadon);
				break;
			case 'Q':
				toRet.add(Deathrattle.Gold_Sated_Threshadon);
				break;
			case 'r':
				toRet.add(Deathrattle.Savannah_Highmane);
				break;
			case 'R':
				toRet.add(Deathrattle.Gold_Savannah_Highmane);
				break;
			case 's':
				toRet.add(Deathrattle.Voidlord);
				break;
			case 'S':
				toRet.add(Deathrattle.Gold_Voidlord);
				break;
			case 't':
				toRet.add(Deathrattle.Ghastcoiler);
				break;
			case 'T':
				toRet.add(Deathrattle.Gold_Ghastcoiler);
				break;
			case 'u':
				toRet.add(Deathrattle.Kangors_Apprentice);
				break;
			case 'U':
				toRet.add(Deathrattle.Gold_Kangors_Apprentice);
				break;
			case 'v':
				toRet.add(Deathrattle.Sneeds_Old_Shredder);
				break;
			case 'V':
				toRet.add(Deathrattle.Gold_Sneeds_Old_Shredder);
				break;
			default:
				Debug.log("If you get this message, you probably fucked up the deathrattle codes", 2);
			}
		}
		
		return toRet;
	}
	
	/**
	 * Creates a DEEP copy of the board given
	 */
	public Board clone() {
		Board toRet = new Board(this.friendTier, this.enemyTier);
		
		toRet.enemies = this.enemies.clone(toRet);
		toRet.friends = this.friends.clone(toRet);
		
		return toRet;
	}
	
	public String toString() {
		return enemies.toString() + "\n" + friends.toString();
	}
	
	public void printBoard() {
		enemies.print();
		System.out.println();
		
		friends.print();
		System.out.println();
		
		System.out.println("\t_F: " + friends._attacking + "; _E" + enemies._attacking);
	}
	
}
