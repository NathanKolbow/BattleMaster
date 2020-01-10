package nkolbow.board;

import java.util.ArrayList;
import java.util.Scanner;

import nkolbow.board.minions.Effect;
import nkolbow.board.minions.Min;
import nkolbow.board.minions.Minion;
import nkolbow.board.minions.Minion.Tribe;
import nkolbow.board.minions.deathrattles.Deathrattle;
import nkolbow.debug.Debug;

public class Board {

	private Line enemies;
	private Line friends;

	public Line getEnemies() {
		return enemies;
	}

	public Line getFriends() {
		return friends;
	}

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
	private static int minDealt = Integer.MAX_VALUE;
	private static int minTaken = Integer.MAX_VALUE;
	private static int totalDealt = 0;
	private static int totalTaken = 0;
	private static int totalUnbiasedDamage = 0;
	private static Board maxTakenBoard;
	private static Board maxDealtBoard;
	private static Board minDealtBoard;
	private static Board minTakenBoard;

	private static Integer[] tScore = new Integer[97];
	
	public static void battle(Board board) {
		for(int i = 0; i < 97; i++)
			tScore[i] = 0;
		
		totalWins = 0;
		totalLosses = 0;
		totalTies = 0;
		maxDealt = 0;
		maxTaken = 0;
		totalDealt = 0;
		totalTaken = 0;
		totalUnbiasedDamage = 0;
		minDealt = Integer.MAX_VALUE;
		minTaken = Integer.MAX_VALUE;

		if (board.enemies.size() > board.friends.size()) {
			Debug.log("Only the enemy can attack first.", 0);
			battleRecur(board, false);
		} else if (board.friends.size() > board.enemies.size()) {
			Debug.log("Only we can attack first.", 0);
			battleRecur(board, true);
		} else {
			Debug.log("Both sides have the chance to attack first.", 0);
			Debug.log("======== FRIENDLY ATTACKING FIRST ========", 1);
			battleRecur(board, true);
			Debug.log("\n\n======== ENEMY ATTACKING FIRST ========", 1);
			battleRecur(board, false);
		}

		if(minDealt == Integer.MAX_VALUE)
			minDealt = 0;
		if(minTaken == Integer.MAX_VALUE)
			minTaken = 0;
		
		System.out.println("\nBATTLE STATS\n" + "=================================================\n"
				+ "Total possibilities: \t" + (totalTies + totalWins + totalLosses) + "\nTotal wins: \t\t" + totalWins
				+ "\t%Chance: ~"
				+ String.format("%.2f", (((double) totalWins * 100) / (double) (totalTies + totalWins + totalLosses)))
				+ "%" + "\nTotal losses: \t\t" + totalLosses + "\t%Chance: ~"
				+ String.format("%.2f", (((double) totalLosses * 100) / (double) (totalTies + totalWins + totalLosses)))
				+ "%" + "\nTotal ties: \t\t" + totalTies + "\t%Chance: ~"
				+ String.format("%.2f", (((double) totalTies * 100) / (double) (totalTies + totalWins + totalLosses)))
				+ "%" + "\nOverall unbiased average damage: "
				+ String.format("%.2f", ((double) (totalUnbiasedDamage)) / (totalWins + totalLosses + totalTies))
				+ "\n\nAverage dealt on wins: " + String.format("%.2f", ((double) totalDealt / (double) totalWins))
				+ "\t\tMax dealt: " + maxDealt + ((maxDealtBoard == null) ? "" : "\n" + maxDealtBoard.toString())
				+ "\nMin dealt: " + minDealt + ((minDealtBoard == null) ? "" : "\n" + minDealtBoard.toString())
				+ "\nAverage taken on losses: " + String.format("%.2f", (double) totalTaken / (double) totalLosses)
				+ "\t\tMax taken: " + maxTaken + ((maxTakenBoard == null) ? "\n" : "\n" + maxTakenBoard.toString())
				+ "\nMin taken: " + minTaken + ((minTakenBoard == null) ? "\n" : "\n" + minTakenBoard.toString())
				+ "\n=================================================");
		
		Scanner in = new Scanner(System.in);
		System.out.print("\nDamage dealt (- if taken): ");
		
		int dmg = in.nextInt();
		if(dmg < 0 || dmg > 96 || tScore[dmg] == 0) {
			System.out.println("That number isn't possible.");
			System.exit(0);
		} else {
			int total = 0;
			for(int i = dmg; i >= 0; i--) {
				total += tScore[i];
			}
			
			System.out.println("Percentile: " + String.format("%.2f", 100*((double)total/(double)(totalWins + totalTies + totalLosses))));
		}
	}

	private static void battleRecur(Board mainBoard, boolean friendAttack) {
		if (mainBoard.enemies.isEmpty()) {
			if (mainBoard.friends.isEmpty()) {
				totalTies++;
				
				tScore[0 + 48]++;
				return;
			} else {
				totalWins++;
				if (mainBoard.friends.getDamage() + mainBoard.friendTier > maxDealt) {
					maxDealt = mainBoard.friends.getDamage() + mainBoard.friendTier;
					maxDealtBoard = mainBoard;
				}
				if (mainBoard.friends.getDamage() + mainBoard.friendTier < minDealt) {
					minDealt = mainBoard.friends.getDamage() + mainBoard.friendTier;
					minDealtBoard = mainBoard;
				}

				totalUnbiasedDamage += mainBoard.friends.getDamage();
				totalDealt += mainBoard.friends.getDamage() + mainBoard.friendTier;
				
				tScore[mainBoard.friends.getDamage() + mainBoard.friendTier]++;
				return;
			}
		} else if (mainBoard.friends.isEmpty()) {
			totalLosses++;
			if (mainBoard.enemies.getDamage() + mainBoard.enemyTier > maxTaken) {
				maxTaken = mainBoard.enemies.getDamage() + mainBoard.enemyTier;
				maxTakenBoard = mainBoard;
			}
			if (mainBoard.enemies.getDamage() + mainBoard.enemyTier < minTaken) {
				minTaken = mainBoard.enemies.getDamage() + mainBoard.enemyTier;
				minTakenBoard = mainBoard;
			}

			totalUnbiasedDamage -= mainBoard.friends.getDamage();
			totalTaken += mainBoard.enemies.getDamage() + mainBoard.enemyTier;
			
			tScore[-(mainBoard.enemies.getDamage() + mainBoard.friendTier) + 48]++;
			return;
		}

		ArrayList<Board> newBoards = new ArrayList<Board>();
		if (friendAttack) {

//System.out.println("Board before:");
//mainBoard.printBoard();
//System.out.println(friendAttack);

			newBoards.addAll(mainBoard.friends.attack(mainBoard, friendAttack));

//System.out.println("Outcome boards (" + newBoards.size() + "):");
//for(Board b : newBoards)
//	b.printBoard();
//
//System.out.println("\n\n\n");

		} else {

//System.out.println("Board before:");
//mainBoard.printBoard();
//System.out.println(friendAttack);

			newBoards.addAll(mainBoard.enemies.attack(mainBoard, friendAttack));

//System.out.println("Outcome boards (" + newBoards.size() + "):");
//for(Board b : newBoards)
//	b.printBoard();
//
//System.out.println("\n\n\n");
		}

//		Debug.log("Total board states found: " + newBoards.size(), 1);
//		for(Board b : newBoards)
//			b.printBoard();

		for (Board b : newBoards) {
			battleRecur(b, !friendAttack);
		}
	}

	public Minion addMinion(boolean isFriend, Min min, boolean golden, int attack, int health, boolean divine,
			boolean poison, boolean taunt) throws FullBoardException {
		return addMinion(isFriend, min, golden, attack, health, divine, poison, taunt, new ArrayList<Deathrattle>());
	}
	
	public Minion addMinion(boolean isFriend, Min min, boolean golden, int attack, int health, boolean divine,
			boolean poison, boolean taunt, Deathrattle rattle) throws FullBoardException {
		ArrayList<Deathrattle> rattles = new ArrayList<Deathrattle>();
		rattles.add(rattle);
		return addMinion(isFriend, min, golden, attack, health, divine, poison, taunt, rattles);
	}
	
	public Minion addMinion(boolean isFriend, Min min, boolean golden, int attack, int health, boolean divine,
			boolean poison, boolean taunt, ArrayList<Deathrattle> deathrattles) throws FullBoardException {
		if ((isFriend) ? friends.size() >= 7 : enemies.size() >= 7) {
			throw new FullBoardException();
		} else {
			deathrattles = (deathrattles == null) ? new ArrayList<Deathrattle>() : deathrattles;

			Tribe tribe = Tribe.None;
			Effect effect = Effect.None;
			boolean hasCleave = false;
			int stars = 0;

			switch (min) {
			case Imp: {
				stars = 1;
				tribe = Tribe.Demon;
				break;
			}
			case Righteous_Protector: {
				stars = 1;
				break;
			}
			case Selfless_Hero: {
				stars = 1;
				break;
			}
			case Wrath_Weaver: {
				stars = 1;
				break;
			}
			case Alleycat: {
				tribe = Tribe.Beast;
				stars = 1;
				break;
			}
			case Dire_Wolf_Alpha: {
				tribe = Tribe.Beast;
				effect = (golden) ? Effect.Gold_Dire_Wolf_Alpha : Effect.Dire_Wolf_Alpha;
				stars = 1;
				break;
			}
			case Voidwalker: {
				tribe = Tribe.Demon;
				stars = 1;
				break;
			}
			case Vulgar_Homunculus: {
				tribe = Tribe.Demon;
				stars = 1;
				break;
			}
			case Mecharoo: {
				tribe = Tribe.Mech;
				stars = 1;
				break;
			}
			case Micro_Machine: {
				tribe = Tribe.Mech;
				stars = 1;
				break;
			}
			case Murloc_Tidecaller: {
				tribe = Tribe.Murloc;
				stars = 1;
				break;
			}
			case Murloc_Tidehunter: {
				tribe = Tribe.Murloc;
				stars = 1;
				break;
			}
			case Rockpool_Hunter: {
				tribe = Tribe.Murloc;
				stars = 1;
				break;
			}
			case Spawn_of_NZoth: {
				stars = 2;
				break;
			}
			case Kindly_Grandmother: {
				tribe = Tribe.Beast;
				stars = 2;
				break;
			}
			case Mounted_Raptor: {
				tribe = Tribe.Beast;
				stars = 2;
				break;
			}
			case Rat_Pack: {
				tribe = Tribe.Beast;
				stars = 2;
				break;
			}
			case Scavenging_Hyena: {
				tribe = Tribe.Beast;
				effect = (golden) ? Effect.Gold_Scavenging_Hyena : Effect.Scavenging_Hyena;
				stars = 2;
				break;
			}
			case Nathrezim_Overseer: {
				tribe = Tribe.Demon;
				stars = 2;
				break;
			}
			case Annoy_o_Tron: {
				tribe = Tribe.Mech;
				stars = 2;
				break;
			}
			case Harvest_Golem: {
				tribe = Tribe.Mech;
				stars = 2;
				break;
			}
			case Kaboom_Bot: {
				tribe = Tribe.Mech;
				stars = 2;
				break;
			}
			case Metaltooth_Leaper: {
				tribe = Tribe.Mech;
				stars = 2;
				break;
			}
			case Pogo_Hopper: {
				tribe = Tribe.Mech;
				stars = 2;
				break;
			}
			case Shielded_Minibot: {
				tribe = Tribe.Mech;
				stars = 2;
				break;
			}
			case Zoobot: {
				tribe = Tribe.Mech;
				stars = 2;
				break;
			}
			case Coldlight_Seer: {
				tribe = Tribe.Murloc;
				stars = 2;
				break;
			}
			case Old_Murk_Eye: {
				stars = 2;
				tribe = Tribe.Murloc;
				effect = (golden) ? Effect.Gold_Old_Murk_Eye : Effect.Old_Murk_Eye;
				break;
			}
			case Murloc_Warleader: {
				stars = 2;
				tribe = Tribe.Murloc;
				effect = (golden) ? Effect.Gold_Murloc_Warleader : Effect.Murloc_Warleader;
				break;
			}
			case Nightmare_Amalgam: {
				stars = 3;
				tribe = Tribe.All;
				break;
			}
			case Crowd_Favorite: {
				stars = 3;
				break;
			}
			case Crystalweaver: {
				stars = 3;
				break;
			}
			case Houndmaster: {
				stars = 3;
				break;
			}
			case Shifter_Zerus: {
				stars = 3;
				break;
			}
			case Tortollan_Shellraiser: {
				stars = 3;
				break;
			}
			case Infested_Wolf: {
				stars = 3;
				tribe = Tribe.Beast;
				break;
			}
			case Imp_Gang_Boss: {
				stars = 3;
				tribe = Tribe.Demon;
				effect = (golden) ? Effect.Gold_Imp_Gang_Boss : Effect.Imp_Gang_Boss;
				break;
			}
			case Floating_Watcher: {
				stars = 3;
				tribe = Tribe.Demon;
				break;
			}
			case Cobalt_Guardian: {
				stars = 3;
				tribe = Tribe.Mech;
				effect = (golden) ? Effect.Gold_Cobalt_Guardian : Effect.Cobalt_Guardian;
				break;
			}
			case Piloted_Shredder: {
				stars = 3;
				tribe = Tribe.Mech;
				break;
			}
			case Psych_o_Tron: {
				stars = 3;
				tribe = Tribe.Mech;
				break;
			}
			case Replicating_Menace: {
				stars = 3;
				tribe = Tribe.Mech;
				break;
			}
			case Screwjank_Clunker: {
				stars = 3;
				tribe = Tribe.Mech;
				break;
			}
			case Khadgar: {
				stars = 3;
				effect = (golden) ? Effect.Gold_Khadgar : Effect.Khadgar;
				break;
			}
			case Pack_Leader: {
				stars = 3;
				effect = (golden) ? Effect.Gold_Pack_Leader : Effect.Pack_Leader;
				break;
			}
			case Phalanx_Commander: {
				stars = 3;
				effect = (golden) ? Effect.Gold_Phalanx_Commander : Effect.Phalanx_Commander;
				break;
			}
			case Soul_Juggler: {
				stars = 3;
				effect = (golden) ? Effect.Gold_Soul_Juggler : Effect.Soul_Juggler;
				break;
			}
			case Defender_of_Argus: {
				stars = 4;
				break;
			}
			case Menagerie_Magician: {
				stars = 4;
				break;
			}
			case Virmen_Sensei: {
				stars = 4;
				break;
			}
			case Bolvar_Fireblood: {
				stars = 4;
				effect = (golden) ? Effect.Gold_Bolvar_Fireblood : Effect.Bolvar_Fireblood;
				break;
			}
			case Festeroot_Hulk: {
				stars = 4;
				effect = (golden) ? Effect.Gold_Festeroot_Hulk : Effect.Festeroot_Hulk;
				break;
			}
			case Cave_Hydra: {
				stars = 4;
				tribe = Tribe.Beast;
				hasCleave = true;
				break;
			}
			case The_Beast: {
				stars = 4;
				tribe = Tribe.Beast;
				break;
			}
			case Siegebreaker: {
				stars = 4;
				tribe = Tribe.Demon;
				effect = (golden) ? Effect.Gold_Siegebreaker : Effect.Siegebreaker;
				break;
			}
			case Annoy_o_Module: {
				stars = 4;
				tribe = Tribe.Mech;
				break;
			}
			case Iron_Sensei: {
				stars = 4;
				tribe = Tribe.Mech;
				break;
			}
			case Piloted_Sky_Golem: {
				stars = 4;
				tribe = Tribe.Mech;
				break;
			}
			case Security_Rover: {
				stars = 4;
				tribe = Tribe.Mech;
				effect = (golden) ? Effect.Gold_Security_Rover : Effect.Security_Rover;
				break;
			}
			case Toxfin: {
				stars = 4;
				tribe = Tribe.Murloc;
				break;
			}
			case Brann_Bronzebeard: {
				stars = 5;
				break;
			}
			case Lightfang_Enforcer: {
				stars = 5;
				break;
			}
			case Strongshell_Scavenger: {
				stars = 5;
				break;
			}
			case Baron_Rivendare: {
				stars = 5;
				effect = (golden) ? Effect.Gold_Baron_Rivendare : Effect.Baron_Rivendare;
				break;
			}
			case Goldrinn_the_Great_Wolf: {
				stars = 5;
				tribe = Tribe.Beast;
				break;
			}
			case Ironhide_Direhorn: {
				stars = 5;
				tribe = Tribe.Beast;
				effect = (golden) ? Effect.Gold_Ironhide_Direhorn : Effect.Ironhide_Direhorn;
				break;
			}
			case Sated_Threshadon: {
				stars = 5;
				tribe = Tribe.Beast;
				break;
			}
			case Savannah_Highmane: {
				stars = 5;
				tribe = Tribe.Beast;
				break;
			}
			case Annihilan_Battlemaster: {
				stars = 5;
				tribe = Tribe.Demon;
				break;
			}
			case MalGanis: {
				stars = 5;
				tribe = Tribe.Demon;
				effect = (golden) ? Effect.Gold_MalGanis : Effect.MalGanis;
				break;
			}
			case Voidlord: {
				stars = 5;
				tribe = Tribe.Demon;
				break;
			}
			case Junkbot: {
				stars = 5;
				tribe = Tribe.Mech;
				effect = (golden) ? Effect.Gold_Junkbot : Effect.Junkbot;
				break;
			}
			case Mechano_Egg: {
				stars = 5;
				tribe = Tribe.Mech;
				break;
			}
			case King_Bagurgle: {
				stars = 5;
				tribe = Tribe.Murloc;
				break;
			}
			case Primalfin_Lookout: {
				stars = 5;
				tribe = Tribe.Murloc;
				break;
			}
			case Kangors_Apprentice: {
				stars = 6;
				break;
			}
			case Zapp_Slywick: {
				stars = 6;
				effect = (golden) ? Effect.Gold_Zapp_Slywick : Effect.Zapp_Slywick;
				break;
			}
			case Gentle_Megasaur: {
				stars = 6;
				tribe = Tribe.Beast;
				break;
			}
			case Ghastcoiler: {
				stars = 6;
				tribe = Tribe.Beast;
				break;
			}
			case Maexxna: {
				stars = 6;
				tribe = Tribe.Beast;
				poison = true;
				break;
			}
			case Mama_Bear: {
				stars = 6;
				tribe = Tribe.Beast;
				effect = (golden) ? Effect.Gold_Mama_Bear : Effect.Mama_Bear;
				break;
			}
			case Foe_Reaper_4000: {
				stars = 6;
				tribe = Tribe.Mech;
				hasCleave = true;
				break;
			}
			case Sneeds_Old_Shredder: {
				stars = 6;
				tribe = Tribe.Mech;
				break;
			}
			default: {
				Debug.log("BIG BAD THIS IS NOT SUPPOSED TO HAPPEN WEE WOO WEE WOO AAAAAAAAAAAAAAAAAAAAAAHHHHHHHHHHHH",
						3);
				Debug.log(min.toString(), 3);
				break;
			}
			}

			Minion toAdd = new Minion(min, golden, tribe, (isFriend) ? friends : enemies, attack, health, health, stars,
					this, effect, divine, poison, hasCleave, taunt, deathrattles);
			
			if(isFriend)
				friends.addLast(toAdd);
			else
				enemies.addLast(toAdd);
			
			return toAdd;
		}
	}

	private Effect readEffects(String str) {
		switch (str) {
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
		switch (str) {
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

		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
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
			case 'w':
				toRet.add(Deathrattle.King_Bagurgle);
				break;
			case 'W':
				toRet.add(Deathrattle.Gold_King_Bagurgle);
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
