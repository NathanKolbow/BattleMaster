package nkolbow.board.minions;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import nkolbow.board.Board;
import nkolbow.board.minions.Minion.Tribe;
import nkolbow.debug.Debug;

public class Line {
	 
	private LinkedList<Minion> minions;
	private LinkedList<Minion> mechDeathOrder;
	// the index of the minion next to attack
	public int _attacking = 0;
	
	public Line() {
		minions = new LinkedList<Minion>();
		mechDeathOrder = new LinkedList<Minion>();
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
			
			ArrayList<SimpleEntry<Integer, ArrayList<Deathrattle>>> enemyDeaths = new ArrayList<SimpleEntry<Integer, ArrayList<Deathrattle>>>();
			for(Minion m : damagedMinions) {
				if(m.isDead(tempEnemies)) {
					enemyDeaths.add(new SimpleEntry<Integer, ArrayList<Deathrattle>>(tempEnemies.minions.indexOf(m), m.getDeathrattles()));
					
					
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
			ArrayList<SimpleEntry<Integer, ArrayList<Deathrattle>>> friendly = new ArrayList<SimpleEntry<Integer, ArrayList<Deathrattle>>>();
			if(attacker.isDead(tempFriends)) {
				friendly.add(new SimpleEntry<Integer, ArrayList<Deathrattle>>(tempFriends.minions.indexOf(attacker), attacker.getDeathrattles()));
			}
			
			ArrayList<Board> toAdd = triggerDeathrattles(friendly, enemyDeaths, newBoard, isFriend);
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
	private ArrayList<Board> triggerDeathrattles(ArrayList<SimpleEntry<Integer, ArrayList<Deathrattle>>> friendlyDeaths, ArrayList<SimpleEntry<Integer, ArrayList<Deathrattle>>> enemyDeaths, Board board, boolean isFriend) {
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
		
		if(friendlyDeaths != null && friendlyDeaths.size() > 0) {
			for(SimpleEntry<Integer, ArrayList<Deathrattle>> entry : friendlyDeaths) {
				int _pos = entry.getKey();
//				Minion friend = tempFriends.minions.get(entry.getKey());
				
				if(_pos == tempFriends._attacking && tempFriends._attacking == tempFriends.minions.size() - 1) {
						tempFriends._attacking = 0;
				}
				
				
				if(friendlyDeaths.get(0).getKey() == tempFriends._attacking && tempFriends._attacking == tempFriends.minions.size() - 1)
					tempFriends._attacking = 0;
			}
		} else {
			tempFriends.incAttacking();
		}
		
		for(SimpleEntry<Integer, ArrayList<Deathrattle>> enemy : enemyDeaths) {
			int _eAttacking = tempEnemies._attacking;
			
			if(_eAttacking > enemy.getKey()) {
				tempEnemies.decAttacking();
			} else if(enemy.getKey() == _eAttacking) {
				if(_eAttacking == tempEnemies.minions.size() - 1)
					tempEnemies._attacking = 0;
				else
					tempEnemies.decAttacking();
			}
		}
		
		
		ArrayList<Minion> toRemove = new ArrayList<Minion>();
		for(SimpleEntry<Integer, ArrayList<Deathrattle>> m : friendlyDeaths) {
			toRemove.add(tempFriends.minions.get(m.getKey()));
		}
		for(Minion m : toRemove) {
			// Add mechs for Kangor's last
			if(m.getTribe() == Tribe.Mech || m.getTribe() == Tribe.All)
				tempFriends.mechDeathOrder.addLast(m.clone(board, tempFriends));

			tempFriends.minions.remove(m);
		}
		
		toRemove = new ArrayList<Minion>();
		for(SimpleEntry<Integer, ArrayList<Deathrattle>> m : enemyDeaths) {
			toRemove.add(tempEnemies.minions.get(m.getKey()));
		}
		for(Minion m : toRemove) {
			// Add mechs for Kangor's last
			if(m.getTribe() == Tribe.Mech || m.getTribe() == Tribe.All)
				tempEnemies.mechDeathOrder.addLast(m.clone(board, tempEnemies));

			tempEnemies.minions.remove(m);
		}
			
		ArrayList<Board> toRet = new ArrayList<Board>();
		toRet.add(board);
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
		if(minions.size() >= 7 || summonList == null || summonList.size() == 0)
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
	
	private void decAttacking() {
		_attacking = Math.max(_attacking - 1, 0);
	}
	
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
