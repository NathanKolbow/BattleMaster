package nkolbow.board.minions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import nkolbow.board.Board;
import nkolbow.board.minions.Minion.Tribe;
import nkolbow.debug.Debug;

public class Line {
	 
	private LinkedList<Minion> minions;
	private LinkedList<Minion> mechDeathOrder;
	private RattleList rattles;	// used when deathrattles are being triggered sequentially
												// probably end up creating a special data structure for this
												// that properly orders deathrattles when they're added
	
	// the index of the minion next to attack
	public int _attacking = 0;
	
	public Line() {
		minions = new LinkedList<Minion>();
		mechDeathOrder = new LinkedList<Minion>();
		rattles = new RattleList();
	}
	
	/**
	 * Goes through all possible attacking permutations
	 * 
	 * @param mainBoard - current board state
	 * @param isFriend - whether or not we are the ones attacking
	 * @return All possible board states that could have emerged from the given board
	 * 	 	   given that we were attacking
	 */
	public ArrayList<Board> attack(Board mainBoard, boolean isFriend) {
		ArrayList<Board> toRet = new ArrayList<Board>();

		Minion __attacker = (isFriend) ? mainBoard.getFriends().minions.get(mainBoard.getFriends()._attacking)
									   : mainBoard.getEnemies().minions.get(mainBoard.getEnemies()._attacking);
		
		int targetSize = (__attacker.getEffect() == Effect.Gold_Zapp_Slywick || __attacker.getEffect() == Effect.Zapp_Slywick)
						? ((isFriend) ? mainBoard.getEnemies().getZappTargets().size() : mainBoard.getFriends().getZappTargets().size())
						: ((isFriend) ? mainBoard.getEnemies().getTargets().size() : mainBoard.getFriends().getTargets().size());
						
		for(int i = 0; i < targetSize; i++) {
			Board newBoard = mainBoard.clone();
			Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();
			Line tempEnemies = (isFriend) ? newBoard.getEnemies() : newBoard.getFriends();

			Minion attacker = tempFriends.minions.get(tempFriends._attacking);
			Minion target = (attacker.getEffect() == Effect.Gold_Zapp_Slywick || attacker.getEffect() == Effect.Zapp_Slywick)
							? tempEnemies.getZappTargets().get(i) : tempEnemies.getTargets().get(i);
			
			ArrayList<Minion> damagedMinions;
			if(attacker.hasCleave()) {
				damagedMinions = getCleaveHits(tempEnemies, target);
			} else {
				damagedMinions = new ArrayList<Minion>();
				damagedMinions.add(target);
			}
			
			for(Minion m : damagedMinions) {
				if(attacker.hasPoison())
					m.takePoison(attacker.getAttack(tempFriends));
				else
					m.takeDamage(attacker.getAttack(tempFriends));
			}
			if(target.hasPoison()) {
				attacker.takePoison(target.getAttack(tempEnemies));
			} else {
				attacker.takeDamage(target.getAttack(tempEnemies));
			}
			
			for(Minion m : damagedMinions) {
				if(m.isDead(tempEnemies)) {
					for(Deathrattle rattle : m.getDeathrattles())
						tempEnemies.rattles.add(new RattleEntry(tempEnemies.minions.indexOf(m), rattle, !isFriend)); // pretty sure !isFriend is correct here
					
/*					if(m.getEffect() == Effect.MalGanis || m.getEffect() == Effect.Gold_MalGanis) {
						for(Minion min : tempEnemies.minions) {
							if(min == m) continue;
							
							int newHealth = Math.min(min.getMaxHealth(), min.getHealth(tempEnemies));
							min.setBaseHealth(newHealth);
						}
					}
					
					// calculate new _attacking
					if(tempEnemies._attacking > tempEnemies.minions.indexOf(m))
						tempEnemies.decAttacking();
					else if(tempEnemies._attacking == tempEnemies.minions.indexOf(m) && tempEnemies._attacking == tempEnemies.minions.size() - 1)
						tempEnemies._attacking = 0; // TODO: remove when deathrattles added*/
				}
			}

/*			for(Minion m : deaths)
				tempEnemies.minions.remove(m);*/
			
/*			if(attacker.isDead(tempFriends)) //{
				deaths.add(attacker);
				if(tempFriends._attacking == tempFriends.minions.indexOf(attacker))
					tempFriends._attacking = 0;
				
				tempFriends.minions.remove(attacker);
			} else {
				// TODO: REMOVE AFTER DEATHRATTLES ARE IMPLEMENTED & CHANGE TO HAVE CLEAVE EVEN BEFORE THAT
				// 		 THIS ++'ing SHOULD ONLY BE DONE IN THE DEATHRATTLE METHOD, WHEN APPROPRIATE
				tempFriends.incAttacking();
			}*/
			
			// TODO: Implement deathrattles
			// Dead minions can't be hit by deathrattles, so removing minions from their linked lists then triggering deathrattles
			// Does that for us implicitly
			if(attacker.isDead(tempFriends)) {
				for(Deathrattle rattle : attacker.getDeathrattles())
					tempFriends.rattles.add(new RattleEntry(tempFriends.minions.indexOf(attacker), rattle, isFriend));
			}
			
			
			ArrayList<Board> toAdd = triggerDeathrattles(newBoard, isFriend);
			toRet.addAll(toAdd);
		}
		
		return toRet;
	}
	
	/**
	 * IMPORTANT NOTE: If _attacking needs to be incremented, THIS method does that, if it does not need to be incremented THIS method *doesn't* do that
	 * 				   THIS INCREMENTING NEEDS TO BE CALCULATED FOR BOTH BOARDS
	 * 
	 * Once deathrattles start, we only need to be able to know which side of the board each minion is from, NOTHING ELSE
	 * This is why there isn't an isFriend boolean.
	 * 
	 * @param deaths - deaths that may or may not have deathrattles to trigger
	 * @return - all possible board states that emerge
	 */
	private ArrayList<Board> triggerDeathrattles(Board board, boolean isFriend) {
		// Game plan: process ALL of the deathrattles in enemyDeaths & friendlyDeaths
		//			  Trigger deathrattles in a set order
		// 			  	  - Perhaps change friendlyDeaths/enemyDeaths to firstProcess/secondProcess for progressive processing of deathrattles??
		//				  - Maybe we only need 1 list for deathrattles though??
		
		/*
		 * ORDER OF DEATHRATTLES:
		 * 		1.  Spawn of N'Zoth
		 * 			Goldrinn, the Great Wolf
		 * 			Selfless Hero
		 * 			Tortollan Shellraiser
		 * 		2.	Kaboom Bot
		 * 		3.	Everything else (order is arbitrary, only summoning happens)
		 */
		
		/* Note: The only deathrattles that will be in a list of deathrattles is either ONLY a single entry from 1
		 *		 Or 2 and one or more 3s, or only one or more 3s
		 *		 Cases:
		 *			1. Only has deathrattle from 1: Trigger the deathrattle, move on
		 *			2. Two and maybe more: Trigger the KABOOM BOT DEATHRATTLE, then trigger remaining in order
		 *			3. Trigger all of these in the order that they appear
		 */
		
		// Currently ignoring deathrattles completely
		Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
		Line tempEnemies = (isFriend) ? board.getEnemies() : board.getFriends();
		
		RattleList friendlyRattles = tempFriends.rattles;
		RattleList enemyRattles = tempEnemies.rattles;
		
		// Absolutely no one died; THIS WILL ALSO BE RUN AT THE VERY END OF RECURSION
/*		if((friendlyRattles == null || friendlyRattles.isEmpty()) && (enemyRattles == null || enemyRattles.isEmpty())) {
			// it probably isn't correct to have this happen
			tempFriends.incAttacking();
			
			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		} else {*/
			// Rivendare triggers even if he dies w/ deathrattles, so gather that information right at the beginning
			int friendlyRiven = 1;
			for(Minion m : tempFriends.minions) {
				if(m.getEffect() == Effect.Baron_Rivendare) {
					if(friendlyRiven == 1)
						friendlyRiven = 2;
				} else if(m.getEffect() == Effect.Gold_Baron_Rivendare) {
					if(friendlyRiven != 3)
						friendlyRiven = 3;
				}
			}
			
			int enemyRiven = 1;
			for(Minion m : tempEnemies.minions) {
				if(m.getEffect() == Effect.Baron_Rivendare) {
					if(enemyRiven == 1)
						enemyRiven = 2;
				} else if(m.getEffect() == Effect.Gold_Baron_Rivendare) {
					if(enemyRiven != 3)
						enemyRiven = 3;
				}
			}
			
			ArrayList<Board> outcomes = new ArrayList<Board>();
			ArrayList<Board> tempOutcomes = new ArrayList<Board>();
			tempOutcomes.add(board); // priming the pump
			
			
			// friends first
			 
			/* 
			 * README ::: friendlyRattles and enemyRattles SHALLOWEST LIST will NEVER be changed past this point,
			 *			  so we can safely iterate through them without fear of them changing in other iterations midway
			 *			  through, then we can just call triggerDeathrattles(...) again on all the outcome boards
			 *			  and continue infinitely, yippee, easy!!
			 */
			while(friendlyRattles.peak().getRattle() != Deathrattle.None) {
				outcomes = tempOutcomes;
				tempOutcomes = new ArrayList<Board>();
				
				for(Board b : outcomes) {
					tempOutcomes.addAll(rattle(b, isFriend, friendlyRiven, enemyRiven));
				}

				friendlyRattles.pop(); // have to do this so that we don't get infinite looped
			}
			
			
			while(enemyRattles.peak().getRattle() != Deathrattle.None) {
				outcomes = tempOutcomes;
				tempOutcomes = new ArrayList<Board>();
				
				for(Board b : outcomes) {
					tempOutcomes.addAll(rattle(b, isFriend, friendlyRiven, enemyRiven));
				}
				
				enemyRattles.pop();
			}
			
			for(Board b : tempOutcomes) {
				if(b.getFriends().rattles.depth.size() == 0)
					b.getFriends().rattles.addDepth();
				if(b.getEnemies().rattles.depth.size() == 0)
					b.getEnemies().rattles.addDepth();
			}
			return tempOutcomes;
//		}
	}
	
	private ArrayList<Board> rattle(Board board, boolean isFriend, int friendlyRiven, int enemyRiven) {
		board = board.clone();
		
		RattleEntry rattle = (isFriend) ? board.getFriends().rattles.pop() : board.getEnemies().rattles.pop();
		int riven = (isFriend) ? friendlyRiven : enemyRiven; // not sure if this is the correct way to pick the riven
		
		switch(rattle.getRattle()) {
		// these first 4 only have 1 possible outcome, so we don't bother with a for loop
		case Process: {
			Debug.log("Processing... " + isFriend, 1);
			
			LinkedList<RattleEntry> newFriendlyDeaths = new LinkedList<RattleEntry>();
			LinkedList<RattleEntry> newEnemyDeaths = new LinkedList<RattleEntry>();
			
			// THIS SHOULD ONLY EVER RETURN 1 BOARD; NAMELY THE BOARD GIVEN ABOVE, JUST WITHOUT THE DEAD MINIONS ON IT
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			Line tempEnemies = (isFriend) ? board.getEnemies() : board.getFriends();
			
			boolean incAttacking = false;
			for(Minion m : tempFriends.minions) {
				int _ind = tempFriends.minions.indexOf(m);
				if(m.isDead(tempFriends)) {
					if(tempFriends._attacking > _ind) {
						tempFriends._attacking--;
					}
					
					for(Deathrattle r : m.getDeathrattles())
						newFriendlyDeaths.add(new RattleEntry(_ind, r, isFriend));

					tempFriends.minions.remove(m);
				} else if(_ind == tempFriends._attacking) {
					incAttacking = true;
				}
			}
			if(incAttacking) tempFriends.incAttacking();
			
			if(tempFriends._attacking == tempFriends.minions.size())
				tempFriends._attacking = 0;
			else if(tempFriends._attacking > tempFriends.minions.size())
				Debug.log("I didn't think it was possible to reach this case. Eek.", 3);
			
			
			for(Minion m : tempEnemies.minions) {
				if(m.isDead(tempEnemies)) {
					int _ind = tempEnemies.minions.indexOf(m);
					if(tempEnemies._attacking > _ind) {
						tempEnemies._attacking--;
					}
					
					int _min = tempEnemies.minions.indexOf(m);
					for(Deathrattle r : m.getDeathrattles())
						newEnemyDeaths.add(new RattleEntry(_min, r, isFriend));
					
					tempEnemies.minions.remove(m);
				}
			}
			if(tempEnemies._attacking == tempEnemies.minions.size())
				tempEnemies._attacking = 0;
			if(tempEnemies._attacking > tempEnemies.minions.size())
				Debug.log("I didn't think it was possible to reach this case. Eek.", 3);
			
			
			friendlyRiven = 1;
			for(Minion m : tempFriends.minions) {
				if(m.getEffect() == Effect.Baron_Rivendare) {
					if(friendlyRiven == 1)
						friendlyRiven = 2;
				} else if(m.getEffect() == Effect.Gold_Baron_Rivendare) {
					if(friendlyRiven != 3)
						friendlyRiven = 3;
				}
			}
			
			enemyRiven = 1;
			for(Minion m : tempEnemies.minions) {
				if(m.getEffect() == Effect.Baron_Rivendare) {
					if(enemyRiven == 1)
						enemyRiven = 2;
				} else if(m.getEffect() == Effect.Gold_Baron_Rivendare) {
					if(enemyRiven != 3)
						enemyRiven = 3;
				}
			}
			
			return rattle(board, isFriend, friendlyRiven, enemyRiven);
		}
		case Spawn_of_NZoth: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			for(int i = 0; i < riven; i++) {
				for(Minion m : tempFriends.getAliveMinions()) {
					m.setBaseAttack(m.getBaseAttack() + 1);
					m.setBaseHealth(m.getBaseHealth() + 1);
				}
			}
			
			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			
			Debug.log("Spawn triggered", 1);
			
			return toRet;
		}
		case Gold_Spawn_of_NZoth: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			for(int i = 0; i < riven; i++) {
				for(Minion m : tempFriends.getAliveMinions()) {
					m.setBaseAttack(m.getBaseAttack() + 2);
					m.setBaseHealth(m.getBaseHealth() + 2);
				}
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Goldrinn_the_Great_Wolf: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			for(int i = 0; i < riven; i++) {
				for(Minion m : tempFriends.getAliveMinions()) {
					if(m.getTribe() == Tribe.Beast) {
						m.setBaseAttack(m.getBaseAttack() + 4);
						m.setBaseHealth(m.getBaseHealth() + 4);
					}
				}
			}
			
			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Goldrinn_the_Great_Wolf: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			for(int i = 0; i < riven; i++) {
				for(Minion m : tempFriends.getAliveMinions()) {
					if(m.getTribe() == Tribe.Beast) {
						m.setBaseAttack(m.getBaseAttack() + 8);
						m.setBaseHealth(m.getBaseHealth() + 8);
					}
				}
			}
			
			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Tortollan_Shellraiser: {
			int iters = (isFriend) ? board.getFriends().getAliveMinions().size() : board.getEnemies().getAliveMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();
			
			for(int i = 0; i < iters; i++) {
				Board newBoard = board.clone();
				
				Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
				Minion toBuff = tempFriends.getAliveMinions().get(i);
				toBuff.setBaseAttack(toBuff.getBaseAttack() + 1);
				toBuff.setBaseHealth(toBuff.getBaseHealth() + 1);
				
				toRet.add(newBoard);
			}
			
			return toRet;
		}
		case None: {
			Debug.log("NONE", 1);
			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		default:
			Debug.log("Something TERRIBLE went wrong with deathrattles.", 3);
			return null;
		}
		
		
	}
	
		// Legacy code: BEFORE deathrattles were implemented, this was the _attacking incrementation/decrementation system
//		if(friendlyDeaths != null && friendlyDeaths.size() > 0) {
//			for(SimpleEntry<Integer, ArrayList<Deathrattle>> entry : friendlyDeaths) {
//				int _pos = entry.getKey();
//				
//				if(_pos == tempFriends._attacking && tempFriends._attacking == tempFriends.minions.size() - 1) {
//						tempFriends._attacking = 0;
//				}
//				
//				
//				if(friendlyDeaths.get(0).getKey() == tempFriends._attacking && tempFriends._attacking == tempFriends.minions.size() - 1)
//					tempFriends._attacking = 0;
//			}
//		} else {
//			tempFriends.incAttacking();
//		}
//		
//		for(SimpleEntry<Integer, ArrayList<Deathrattle>> enemy : enemyDeaths) {
//			int _eAttacking = tempEnemies._attacking;
//			
//			if(_eAttacking > enemy.getKey()) {
//				tempEnemies.decAttacking();
//			} else if(enemy.getKey() == _eAttacking) {
//				if(_eAttacking == tempEnemies.minions.size() - 1)
//					tempEnemies._attacking = 0;
//				else
//					tempEnemies.decAttacking();
//			}
//		}
//		
//		
//		ArrayList<Minion> toRemove = new ArrayList<Minion>();
//		for(SimpleEntry<Integer, ArrayList<Deathrattle>> m : friendlyDeaths) {
//			toRemove.add(tempFriends.minions.get(m.getKey()));
//		}
//		for(Minion m : toRemove) {
//			// Add mechs for Kangor's last
//			if(m.getTribe() == Tribe.Mech || m.getTribe() == Tribe.All)
//				tempFriends.mechDeathOrder.addLast(m.clone(board, tempFriends));
//
//			tempFriends.minions.remove(m);
//		}
//		
//		toRemove = new ArrayList<Minion>();
//		for(SimpleEntry<Integer, ArrayList<Deathrattle>> m : enemyDeaths) {
//			toRemove.add(tempEnemies.minions.get(m.getKey()));
//		}
//		for(Minion m : toRemove) {
//			// Add mechs for Kangor's last
//			if(m.getTribe() == Tribe.Mech || m.getTribe() == Tribe.All)
//				tempEnemies.mechDeathOrder.addLast(m.clone(board, tempEnemies));
//
//			tempEnemies.minions.remove(m);
//		}
//			
//		ArrayList<Board> toRet = new ArrayList<Board>();
//		toRet.add(board);
//		return toRet;
//	}
	
	public ArrayList<Minion> getAliveMinions() {
		ArrayList<Minion> toRet = new ArrayList<Minion>();
		for(Minion m : minions)
			if(!m.isDead(this))
				toRet.add(m);
		
		return toRet;
	}
	
	public int summon(Minion toSummon, int _pos, Line from) {
		ArrayList<Minion> list = new ArrayList<Minion>();
		list.add(toSummon);
		return summon(list, _pos, from, getKhadgarMultiplier(from));
	}
	
	public int summon(ArrayList<Minion> summonList, int _pos, Line from) {
		return summon(summonList, _pos, from, getKhadgarMultiplier(from));
	}
	
	/**
	 * Processes all of the buffs that ANY GIVEN MINION(S) gets when a minion is summoned, and then
	 * summons the actual minion(s)
	 * 
	 * @param summonList - list of minions to be summoned
	 * @param _pos       - position to summon minions at
	 * @param from       - line that the minions are being summoned from (required b/c of The Beast)
	 * @param khadgar    - used to deal w/ the weird situation w/ Khadgar summoning copies of already buffed minions
	 * @return
	 */
	public int summon(ArrayList<Minion> summonList, int _pos, Line from, int khadgar) {
		if(khadgar == 0)
			return 0;
		if(getAliveMinions().size() >= 7 || summonList == null || summonList.size() == 0)
			return 0;
		
		Minion toSummon = summonList.get(0);
		if(toSummon.getTribe() == Tribe.Mech || toSummon.getTribe() == Tribe.All) {
			for(Minion m : minions) {
				if(m.getEffect() == Effect.Cobalt_Guardian || m.getEffect() == Effect.Gold_Cobalt_Guardian)
					m.setDivine(true);
			}
		} else if(toSummon.getTribe() == Tribe.Beast || toSummon.getTribe() == Tribe.All) {
			for(Minion m : minions) {
				if(m.getEffect() == Effect.Pack_Leader)
					toSummon.setBaseAttack(toSummon.getBaseAttack() + 3);
				if(m.getEffect() == Effect.Gold_Pack_Leader)
					toSummon.setBaseAttack(toSummon.getBaseAttack() + 6);
				if(m.getEffect() == Effect.Mama_Bear) {
					toSummon.setBaseAttack(toSummon.getBaseAttack() + 4);
					toSummon.setBaseHealth(toSummon.getBaseHealth() + 4);
				}
				if(m.getEffect() == Effect.Gold_Mama_Bear) {
					toSummon.setBaseAttack(toSummon.getBaseAttack() + 8);
					toSummon.setBaseHealth(toSummon.getBaseHealth() + 8);
				}
			}
		}
		
		int toRet = 0;
		for(Minion m : summonList) {
			if(minions.size() < 7) {
				minions.add(Math.min(minions.size(), _pos), m);
				toRet++;
			}
			
			for(int i = 1; i < khadgar; i++) {
				ArrayList<Minion> list = new ArrayList<Minion>();
				list.add(m.clone(m.getBoard(), m.getLine()));
				toRet += summon(list, _pos, from, 1);
			}
		}
		
		return toRet;
	}
	
	/**
	 * Minions getter
	 * @return - list of minions
	 */
	public LinkedList<Minion> getMinions() {
		return this.minions;
	}
	
	/**
	 * Returns ONLY the minions adjacent to the given minion
	 * 
	 * @param m - minion to get minions adjacent to
	 * @return  - minions adjacent to m
	 */
	public ArrayList<Minion> getAdjMinions(Minion m) {
		int _ind = this.minions.indexOf(m);
		if(_ind < 0)
			throw new NoSuchElementException();
		
		ArrayList<Minion> toRet = new ArrayList<Minion>();
		if(_ind == 0) {
			if(minions.size() > 1)
				toRet.add(minions.get(_ind + 1));
		} else if(_ind == minions.size() - 1) {
			if(minions.size() != 1)
				toRet.add(minions.get(minions.size() - 2));
		} else {
			toRet.add(minions.get(_ind + 1));
			toRet.add(minions.get(_ind - 1));
		}
		
		return toRet;
	}
	
	/**
	 * Returns the number of minions to be summoned according to whether there is a khadgar/gold khadgar
	 * or not
	 * 
	 * @param l - line that is doing the summoning (needed b/c of The Beast)
	 * @return  - total number of minions to be summoned; [1, 3]
	 */
	private int getKhadgarMultiplier(Line l) {
		int mult = 1;
		for(Minion m : l.minions) {
			if(m.getEffect() == Effect.Khadgar && mult < 2)
				mult = 2;
			if(m.getEffect() == Effect.Gold_Khadgar && mult < 3)
				mult = 3;
		}
		
		return mult;
	}
	
/*	private void decAttacking() {
		_attacking = Math.max(_attacking - 1, 0);
	}*/
	
	private void incAttacking() {
		_attacking = (_attacking + 1) % minions.size();
	}
	
	/**
	 * Gets the minions that a cleave would hit
	 * 
	 * @param line - line that the minion being hit is from
	 * @param m - minion being hit
	 * @return - all minions that would be hit by a cleave
	 */
	public ArrayList<Minion> getCleaveHits(Line line, Minion m) {
		int ind = line.minions.indexOf(m);
		ArrayList<Minion> toRet = new ArrayList<Minion>();
		toRet.add(m);
		
		if(ind == 0) {
			if(line.minions.size() == 1)
				return toRet;
			toRet.add(line.minions.get(ind + 1));
			return toRet;
		} else if(ind == line.minions.size() - 1) {
			toRet.add(line.minions.get(ind - 1));
			return toRet;
		} else {
			toRet.add(line.minions.get(ind - 1));
			toRet.add(line.minions.get(ind + 1));
			return toRet;
		}
	}
	
	/**
	 * Creates a DEEP clone of itself
	 */
	public Line clone(Board b) {
		Line toRet = new Line();
		for(Minion m : minions)
			toRet.minions.addLast(m.clone(b, toRet));
		
		for(Minion m : mechDeathOrder)
			toRet.mechDeathOrder.addLast(m.clone(b, toRet));
		
		toRet.rattles = rattles.clone();
		toRet._attacking = this._attacking;
		
		return toRet;
	}
	
	public ArrayList<Minion> getZappTargets() {
		int lowest = Integer.MAX_VALUE;
		ArrayList<Minion> toRet = new ArrayList<Minion>();
		
		for(Minion m : this.minions) {
			if(m.getAttack(this) == lowest) {
				toRet.add(m);
			} else if(m.getAttack(this) < lowest) {
				toRet = new ArrayList<Minion>();
				toRet.add(m);
			}
		}
		
		return toRet;
	}
	
	public ArrayList<Minion> getTargets() {
		boolean taunt = false;
		ArrayList<Minion> toRet = new ArrayList<Minion>();
		
		for(Minion m : this.minions) {
			if(m.hasTaunt()) {
				if(taunt) {
					toRet.add(m);
				} else {
					taunt = true;
					toRet = new ArrayList<Minion>();
					toRet.add(m);
				}
			} else {
				if(!taunt)
					toRet.add(m);
			}
		}
		
		return toRet;
	}
	
	public int getDamage() {
		int toRet = 0;
		for(Minion m : minions) {
			toRet += m.getStars();
		}
		
		// TODO: add hero tavern level
		return toRet;
	}
	
	public void add(int p, Minion m) { minions.add(p, m); }
	public void addLast(Minion m) { minions.addLast(m); }
	public int size() { return minions.size(); }
	public boolean isEmpty() { return minions.isEmpty(); }
	
	public String toString() {
		String toRet = "";
		if(minions.size() == 0)
			toRet += "\t<EMPTY>";
		else
			for(Minion m : minions)
				toRet += "\t" + m.toString();
			
		return toRet;
	}
	
	public void print() {
		for(Minion m : minions)
			System.out.print("\t" + m.toString());
	}
	
}
