package nkolbow.board;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import nkolbow.board.minions.Effect;
import nkolbow.board.minions.Min;
import nkolbow.board.minions.Minion;
import nkolbow.board.minions.Minion.Tribe;
import nkolbow.board.minions.deathrattles.Deathrattle;
import nkolbow.board.minions.deathrattles.RattleEntry;
import nkolbow.board.minions.deathrattles.RattleList;
import nkolbow.debug.Debug;

public class Line {

	private LinkedList<Minion> minions;
	private LinkedList<Minion> mechDeathOrder;
	private RattleList rattles; // used when deathrattles are being triggered sequentially
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
	 * @param isFriend  - whether or not we are the ones attacking
	 * @return All possible board states that could have emerged from the given
	 *         board given that we were attacking
	 */
	public ArrayList<Board> attack(Board mainBoard, boolean isFriend) {
		ArrayList<Board> toRet = new ArrayList<Board>();

		Minion __attacker = (isFriend) ? mainBoard.getFriends().minions.get(mainBoard.getFriends()._attacking)
				: mainBoard.getEnemies().minions.get(mainBoard.getEnemies()._attacking);

		int targetSize = (__attacker.getEffect() == Effect.Gold_Zapp_Slywick
				|| __attacker.getEffect() == Effect.Zapp_Slywick)
						? ((isFriend) ? mainBoard.getEnemies().getZappTargets().size()
								: mainBoard.getFriends().getZappTargets().size())
						: ((isFriend) ? mainBoard.getEnemies().getTargets().size()
								: mainBoard.getFriends().getTargets().size());

		for (int i = 0; i < targetSize; i++) {
			Board newBoard = mainBoard.clone();
			Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();
			Line tempEnemies = (isFriend) ? newBoard.getEnemies() : newBoard.getFriends();

			Minion attacker = tempFriends.minions.get(tempFriends._attacking);
			Minion target = (attacker.getEffect() == Effect.Gold_Zapp_Slywick
					|| attacker.getEffect() == Effect.Zapp_Slywick) ? tempEnemies.getZappTargets().get(i)
							: tempEnemies.getTargets().get(i);

			ArrayList<Minion> damagedMinions;
			if (attacker.hasCleave()) {
				damagedMinions = getCleaveHits(tempEnemies, target);
			} else {
				damagedMinions = new ArrayList<Minion>();
				damagedMinions.add(target);
			}

			for (Minion m : damagedMinions) {
				if (attacker.hasPoison())
					m.takePoison(attacker.getAttack(tempFriends));
				else
					m.takeDamage(attacker.getAttack(tempFriends));
			}
			if (target.hasPoison()) {
				attacker.takePoison(target.getAttack(tempEnemies));
			} else {
				attacker.takeDamage(target.getAttack(tempEnemies));
			}

			// Process overkill-type mechanics here
			if (attacker.getEffect() == Effect.Ironhide_Direhorn
					|| attacker.getEffect() == Effect.Gold_Ironhide_Direhorn) {
				for (Minion m : damagedMinions) {
					if (m.isDead(tempEnemies)) {
						int stat = (attacker.getEffect() == Effect.Ironhide_Direhorn) ? 5 : 10;
						Minion token = new Minion(Min.Ironhide_Token, attacker.getIsGolden(), Tribe.Beast, tempFriends, stat, stat, stat, 1,
								newBoard, Effect.None, false, false, false, false, new ArrayList<Deathrattle>());

						tempFriends.summon(token, tempFriends.minions.indexOf(attacker), tempFriends);
					}
				}
			} else if (attacker.getEffect() == Effect.The_Boogeymonster
					|| attacker.getEffect() == Effect.Gold_The_Boogeymonster) {
				if (!attacker.isDead(tempFriends)) {
					for (Minion m : damagedMinions) {
						if (m.isDead(tempEnemies)) {
							int gain = (attacker.getEffect() == Effect.The_Boogeymonster) ? 2 : 4;
							attacker.setBaseAttack(attacker.getBaseAttack() + gain);
							attacker.setBaseHealth(attacker.getBaseHealth() + gain);
						}
					}
				}
			}

			for (Minion m : damagedMinions) {
				if (m.isDead(tempEnemies)) {
					for (Deathrattle rattle : m.getDeathrattles())
						tempEnemies.rattles.add(new RattleEntry(tempEnemies.minions.indexOf(m), rattle, m));
				}
			}

			/*
			 * for(Minion m : deaths) tempEnemies.minions.remove(m);
			 */

			/*
			 * if(attacker.isDead(tempFriends)) //{ deaths.add(attacker);
			 * if(tempFriends._attacking == tempFriends.minions.indexOf(attacker))
			 * tempFriends._attacking = 0;
			 * 
			 * tempFriends.minions.remove(attacker); } else { // TODO: REMOVE AFTER
			 * DEATHRATTLES ARE IMPLEMENTED & CHANGE TO HAVE CLEAVE EVEN BEFORE THAT // THIS
			 * ++'ing SHOULD ONLY BE DONE IN THE DEATHRATTLE METHOD, WHEN APPROPRIATE
			 * tempFriends.incAttacking(); }
			 */

			// TODO: Implement deathrattles
			// Dead minions can't be hit by deathrattles, so removing minions from their
			// linked lists then triggering deathrattles
			// Does that for us implicitly
			if (attacker.isDead(tempFriends)) {
				for (Deathrattle rattle : attacker.getDeathrattles())
					tempFriends.rattles.add(new RattleEntry(tempFriends.minions.indexOf(attacker), rattle, attacker));
			}

			ArrayList<Board> toAdd = triggerDeathrattles(newBoard, isFriend);
			toRet.addAll(toAdd);
		}

		return toRet;
	}

	/**
	 * IMPORTANT NOTE: If _attacking needs to be incremented, THIS method does that,
	 * if it does not need to be incremented THIS method *doesn't* do that THIS
	 * INCREMENTING NEEDS TO BE CALCULATED FOR BOTH BOARDS
	 * 
	 * Once deathrattles start, we only need to be able to know which side of the
	 * board each minion is from, NOTHING ELSE This is why there isn't an isFriend
	 * boolean.
	 * 
	 * @param deaths - deaths that may or may not have deathrattles to trigger
	 * @return - all possible board states that emerge
	 */
	private ArrayList<Board> triggerDeathrattles(Board board, boolean isFriend) {
		// Game plan: process ALL of the deathrattles in enemyDeaths & friendlyDeaths
		// Trigger deathrattles in a set order
		// - Perhaps change friendlyDeaths/enemyDeaths to firstProcess/secondProcess for
		// progressive processing of deathrattles??
		// - Maybe we only need 1 list for deathrattles though??

		/*
		 * ORDER OF DEATHRATTLES: 1. Spawn of N'Zoth Goldrinn, the Great Wolf Selfless
		 * Hero Tortollan Shellraiser 2. Kaboom Bot 3. Everything else (order is
		 * arbitrary, only summoning happens)
		 */

		/*
		 * Note: The only deathrattles that will be in a list of deathrattles is either
		 * ONLY a single entry from 1 Or 2 and one or more 3s, or only one or more 3s
		 * Cases: 1. Only has deathrattle from 1: Trigger the deathrattle, move on 2.
		 * Two and maybe more: Trigger the KABOOM BOT DEATHRATTLE, then trigger
		 * remaining in order 3. Trigger all of these in the order that they appear
		 */

		// Currently ignoring deathrattles completely
		Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
		Line tempEnemies = (isFriend) ? board.getEnemies() : board.getFriends();

		RattleList friendlyRattles = tempFriends.rattles;
		RattleList enemyRattles = tempEnemies.rattles;

		// Absolutely no one died; THIS WILL ALSO BE RUN AT THE VERY END OF RECURSION
		/*
		 * if((friendlyRattles == null || friendlyRattles.isEmpty()) && (enemyRattles ==
		 * null || enemyRattles.isEmpty())) { // it probably isn't correct to have this
		 * happen tempFriends.incAttacking();
		 * 
		 * ArrayList<Board> toRet = new ArrayList<Board>(); toRet.add(board); return
		 * toRet; } else {
		 */
		// Rivendare triggers even if he dies w/ deathrattles, so gather that
		// information right at the beginning
		int friendlyRiven = 1;
		for (Minion m : tempFriends.minions) {
			if (m.getEffect() == Effect.Baron_Rivendare) {
				if (friendlyRiven == 1)
					friendlyRiven = 2;
			} else if (m.getEffect() == Effect.Gold_Baron_Rivendare) {
				if (friendlyRiven != 3)
					friendlyRiven = 3;
			}
		}

		int enemyRiven = 1;
		for (Minion m : tempEnemies.minions) {
			if (m.getEffect() == Effect.Baron_Rivendare) {
				if (enemyRiven == 1)
					enemyRiven = 2;
			} else if (m.getEffect() == Effect.Gold_Baron_Rivendare) {
				if (enemyRiven != 3)
					enemyRiven = 3;
			}
		}

		ArrayList<Board> outcomes = new ArrayList<Board>();
		ArrayList<Board> tempOutcomes = new ArrayList<Board>();
		tempOutcomes.add(board); // priming the pump

		// friends first

		/*
		 * README ::: friendlyRattles and enemyRattles SHALLOWEST LIST will NEVER be
		 * changed past this point, so we can safely iterate through them without fear
		 * of them changing in other iterations midway through, then we can just call
		 * triggerDeathrattles(...) again on all the outcome boards and continue
		 * infinitely, yippee, easy!!
		 */
		while (friendlyRattles.peak().getRattle() != Deathrattle.Process) {
			outcomes = tempOutcomes;
			tempOutcomes = new ArrayList<Board>();

			for (Board b : outcomes) {
				tempOutcomes.addAll(rattle(b, isFriend, friendlyRiven, enemyRiven));
			}

			friendlyRattles.pop(); // have to do this so that we don't get infinite looped
		}

		while (enemyRattles.peak().getRattle() != Deathrattle.Process) {
			outcomes = tempOutcomes;
			tempOutcomes = new ArrayList<Board>();

			for (Board b : outcomes) {
				tempOutcomes.addAll(rattle(b, !isFriend, enemyRiven, friendlyRiven));
			}

			enemyRattles.pop();
		}

		outcomes = tempOutcomes;
		tempOutcomes = new ArrayList<Board>();
		for (Board b : outcomes) {
			tempOutcomes.addAll(rattle(b, isFriend, friendlyRiven, enemyRiven));
		}

		for (Board b : tempOutcomes) {
			if (b.getFriends().rattles.depth.size() == 0)
				b.getFriends().rattles.addDepth();
			if (b.getEnemies().rattles.depth.size() == 0)
				b.getEnemies().rattles.addDepth();
		}

		return tempOutcomes;
//		}
	}

	private ArrayList<Board> rattle(Board board, boolean isFriend, int friendlyRiven, int enemyRiven) {
		board = board.clone();

		RattleEntry rattle = (isFriend) ? board.getFriends().rattles.pop() : board.getEnemies().rattles.pop();
		switch (rattle.getRattle()) {
		// these first 4 only have 1 possible outcome, so we don't bother with a for
		// loop
		case Process: {
			LinkedList<RattleEntry> newFriendlyDeaths = new LinkedList<RattleEntry>();
			LinkedList<RattleEntry> newEnemyDeaths = new LinkedList<RattleEntry>();

			// THIS SHOULD ONLY EVER RETURN 1 BOARD; NAMELY THE BOARD GIVEN ABOVE, JUST
			// WITHOUT THE DEAD MINIONS ON IT
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			Line tempEnemies = (isFriend) ? board.getEnemies() : board.getFriends();

			boolean incAttacking = false;
			LinkedList<Minion> toRemove = new LinkedList<Minion>();
			int toDec = 0;

			for (Minion m : tempFriends.minions) {
				int _ind = tempFriends.minions.indexOf(m);
				if (m.isDead(tempFriends)) {
					if (tempFriends._attacking > _ind) {
						toDec++;
					}

					for (Deathrattle r : m.getDeathrattles())
						newFriendlyDeaths.add(new RattleEntry(_ind, r, m));

					toRemove.add(m);
				} else if (_ind == tempFriends._attacking) {
					incAttacking = true;
				}
			}
			tempFriends.minions.removeAll(toRemove);
			
			// process things like JunkBot
			for(Minion alive : tempFriends.getAliveMinions()) {
				if(alive.getEffect() == Effect.Junkbot || alive.getEffect() == Effect.Gold_Junkbot) {
					for(Minion dead : toRemove) {
						if(dead.getTribe() == Tribe.Mech) {
							int gain = (alive.getEffect() == Effect.Junkbot) ? 2 : 4;
							alive.setBaseAttack(alive.getBaseAttack() + gain);
							alive.setBaseHealth(alive.getBaseHealth() + gain);
						}
					}
				}  /*
					* Soul juggler shit PROBABLY goes here, looks like we're gonna be adding another deathrattle for juggler hitting shit, b/c it's effectively a deathrattle
					* YES YES YES that's so much easier, just give every demon the deathrattle: Deathrattle.Demon and process it like a normal deathrattle,
					* check for soul jugglers and shoot the juggles as if they were literally just deathrattles from the demon that died
					*/
			}
			
			tempFriends._attacking -= toDec;
			if (incAttacking)
				tempFriends.incAttacking();

			if (tempFriends._attacking == tempFriends.minions.size())
				tempFriends._attacking = 0;
			else if (tempFriends._attacking > tempFriends.minions.size())
				Debug.log("I didn't think it was possible to reach this case. Eek.", 3);

			toRemove = new LinkedList<Minion>();
			toDec = 0;
			for (Minion m : tempEnemies.minions) {
				if (m.isDead(tempEnemies)) {
					int _ind = tempEnemies.minions.indexOf(m);
					if (tempEnemies._attacking > _ind) {
						toDec++;
					}

					int _min = tempEnemies.minions.indexOf(m);
					for (Deathrattle r : m.getDeathrattles())
						newEnemyDeaths.add(new RattleEntry(_min, r, m));

					toRemove.add(m);
				}
			}
			tempEnemies._attacking -= toDec;
			tempEnemies.minions.removeAll(toRemove);
			if (tempEnemies._attacking == tempEnemies.minions.size())
				tempEnemies._attacking = 0;
			if (tempEnemies._attacking > tempEnemies.minions.size())
				Debug.log("I didn't think it was possible to reach this case. Eek.", 3);

			friendlyRiven = 1;
			for (Minion m : (isFriend) ? tempFriends.minions : tempEnemies.minions) {
				if (m.getEffect() == Effect.Baron_Rivendare) {
					if (friendlyRiven == 1)
						friendlyRiven = 2;
				} else if (m.getEffect() == Effect.Gold_Baron_Rivendare) {
					if (friendlyRiven != 3)
						friendlyRiven = 3;
				}
			}

			enemyRiven = 1;
			for (Minion m : (isFriend) ? tempEnemies.minions : tempFriends.minions) {
				if (m.getEffect() == Effect.Baron_Rivendare) {
					if (enemyRiven == 1)
						enemyRiven = 2;
				} else if (m.getEffect() == Effect.Gold_Baron_Rivendare) {
					if (enemyRiven != 3)
						enemyRiven = 3;
				}
			}

			return rattle(board, isFriend, friendlyRiven, enemyRiven);
		}
		case Spawn_of_NZoth: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			for (int i = 0; i < friendlyRiven; i++) {
				for (Minion m : tempFriends.getAliveMinions()) {
					m.setBaseAttack(m.getBaseAttack() + friendlyRiven); // + (1 * friendlyRiven)
					m.setBaseHealth(m.getBaseHealth() + friendlyRiven);
				}
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Spawn_of_NZoth: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			for (int i = 0; i < friendlyRiven; i++) {
				for (Minion m : tempFriends.getAliveMinions()) {
					m.setBaseAttack(m.getBaseAttack() + (2 * friendlyRiven));
					m.setBaseHealth(m.getBaseHealth() + (2 * friendlyRiven));
				}
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Goldrinn_the_Great_Wolf: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			for (int i = 0; i < friendlyRiven; i++) {
				for (Minion m : tempFriends.getAliveMinions()) {
					if (m.getTribe() == Tribe.Beast || m.getTribe() == Tribe.All) {
						m.setBaseAttack(m.getBaseAttack() + (4 * friendlyRiven));
						m.setBaseHealth(m.getBaseHealth() + (4 * friendlyRiven));
					}
				}
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Goldrinn_the_Great_Wolf: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			for (int i = 0; i < friendlyRiven; i++) {
				for (Minion m : tempFriends.getAliveMinions()) {
					if (m.getTribe() == Tribe.Beast || m.getTribe() == Tribe.All) {
						m.setBaseAttack(m.getBaseAttack() + (8 * friendlyRiven));
						m.setBaseHealth(m.getBaseHealth() + (8 * friendlyRiven));
					}
				}
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Kaboom_Bot: {
			int choices = (isFriend) ? board.getEnemies().getAliveMinions().size()
					: board.getFriends().getAliveMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();
			if (choices == 0) {
				toRet.add(board);
				return toRet;
			}

			if (friendlyRiven == 1) {
				for (int i = 0; i < choices; i++) {
					Debug.log((rattle.getSource().getLine() == board.getEnemies()) ? "ENEMY" : "FRIEND", 3);
					
					Board newBoard = board.clone();
					Line tempEnemies = (isFriend) ? newBoard.getEnemies() : newBoard.getFriends();

					Minion toHit = tempEnemies.getAliveMinions().get(i);
					toHit.takeDamage(4);

					toRet.add(newBoard);
				}

				return toRet;
			} else if (friendlyRiven == 2) {
				for (int i = 0; i < choices; i++) {
					Board newBoard1 = board.clone();
					Line tempEnemies1 = (isFriend) ? newBoard1.getEnemies() : newBoard1.getFriends();
					
					Minion hitOne = tempEnemies1.getAliveMinions().get(i);
					hitOne.takeDamage(4);
					
					int secondChoices = tempEnemies1.getAliveMinions().size();
					if(secondChoices == 0) {
						toRet.add(newBoard1);
						return toRet;
					}
					
					for (int j = 0; j < secondChoices; j++) {
						Board newBoard2 = newBoard1.clone();
						Line tempEnemies2 = (isFriend) ? newBoard2.getEnemies() : newBoard2.getFriends();
						
						Minion hitTwo = tempEnemies2.getAliveMinions().get(j);
						hitTwo.takeDamage(4);
						
						toRet.add(newBoard2);
					}
				}
			} else if (friendlyRiven == 3) {
				for (int i = 0; i < choices; i++) {
					Board newBoard1 = board.clone();
					Line tempEnemies1 = (isFriend) ? newBoard1.getEnemies() : newBoard1.getFriends();
					
					Minion hitOne = tempEnemies1.getAliveMinions().get(i);
					hitOne.takeDamage(4);
					
					int secondChoices = tempEnemies1.getAliveMinions().size();
					if(secondChoices == 0) {
						toRet.add(newBoard1);
					}
					
					for(int j = 0; j < secondChoices; j++) {
						Board newBoard2 = newBoard1.clone();
						Line tempEnemies2 = (isFriend) ? newBoard2.getEnemies() : newBoard2.getFriends();
						
						Minion hitTwo = tempEnemies2.getAliveMinions().get(j);
						hitTwo.takeDamage(4);
						
						int threeChoices = tempEnemies2.getAliveMinions().size();
						if(threeChoices == 0) {
							toRet.add(newBoard2);
						}
						
						for(int k = 0; k < threeChoices; k++) {
							Board newBoard3 = newBoard2.clone();
							Line tempEnemies3 = (isFriend) ? newBoard3.getEnemies() : newBoard3.getFriends();
							
							Minion hitThree = tempEnemies3.getAliveMinions().get(k);
							hitThree.takeDamage(4);
							
							toRet.add(newBoard3);
						}
					}
				}
			}

			return toRet;
		}
		case Gold_Kaboom_Bot: {
			int choices = (isFriend) ? board.getEnemies().getAliveMinions().size()
					: board.getFriends().getAliveMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();
			if (choices == 0) {
				toRet.add(board);
				return toRet;
			}

			if (friendlyRiven == 1) {
				for(int i = 0; i < choices; i++) {
					Board newBoard = board.clone();
					Line tempEnemies = (isFriend) ? newBoard.getEnemies() : newBoard.getFriends();
					
					Minion hitOne = tempEnemies.getAliveMinions().get(i);
					hitOne.takeDamage(4);
					
					int secondChoices = tempEnemies.getAliveMinions().size();
					if(secondChoices == 0) {
						toRet.add(newBoard);
					}
					
					for(int j = 0; j < secondChoices; j++) {
						Board newBoard2 = newBoard.clone();
						Line tempEnemies2 = (isFriend) ? newBoard2.getEnemies() : newBoard2.getFriends();
						
						Minion hitTwo = tempEnemies2.getAliveMinions().get(j);
						hitTwo.takeDamage(4);
						
						toRet.add(newBoard2);
					}
				}
			} else if (friendlyRiven == 2) {
				for (int i1 = 0; i1 < choices; i1++) {
					Board newBoard1 = board.clone();
					Line tempEnemies1 = (isFriend) ? newBoard1.getEnemies() : newBoard1.getFriends();
					
					Minion hitOne = tempEnemies1.getAliveMinions().get(i1);
					hitOne.takeDamage(4);
					
					int oneSecondChoices = tempEnemies1.getAliveMinions().size();
					if(oneSecondChoices == 0) {
						toRet.add(newBoard1);
					}
					
					for (int i2 = 0; i2 < oneSecondChoices; i2++) {
						Board newBoard2 = newBoard1.clone();
						Line tempEnemies2 = (isFriend) ? newBoard2.getEnemies() : newBoard2.getFriends();
						
						Minion hitTwo = tempEnemies2.getAliveMinions().get(i2);
						hitTwo.takeDamage(4);
						
						int twoFirstChoices = tempEnemies2.getAliveMinions().size();
						if(twoFirstChoices == 0) {
							toRet.add(newBoard2);
						}
						
						for(int j1 = 0; j1 < twoFirstChoices; j1++) {
							Board newBoard3 = newBoard2.clone();
							Line tempEnemies3 = (isFriend) ? newBoard3.getEnemies() : newBoard3.getFriends();
							
							Minion hitThree = tempEnemies3.getAliveMinions().get(j1);
							hitThree.takeDamage(4);
							
							int twoSecondChoices = tempEnemies3.getAliveMinions().size();
							if(twoSecondChoices == 0) {
								toRet.add(newBoard3);
							}
							
							for(int j2 = 0; j2 < twoSecondChoices; j2++) {
								Board newBoard4 = newBoard3.clone();
								Line tempEnemies4 = (isFriend) ? newBoard4.getEnemies() : newBoard4.getFriends();
								
								Minion hitFour = tempEnemies4.getAliveMinions().get(j2);
								hitFour.takeDamage(4);
								
								toRet.add(newBoard4);
							}
						}
					}
				}
			} else if (friendlyRiven == 3) {
				for (int i1 = 0; i1 < choices; i1++) {
					Board newBoard1 = board.clone();
					Line tempEnemies1 = (isFriend) ? newBoard1.getEnemies() : newBoard1.getFriends();
					
					Minion hitOne = tempEnemies1.getAliveMinions().get(i1);
					hitOne.takeDamage(4);
					
					int oneSecondChoices = tempEnemies1.getAliveMinions().size();
					if(oneSecondChoices == 0) {
						toRet.add(newBoard1);
					}
					
					for (int i2 = 0; i2 < oneSecondChoices; i2++) {
						Board newBoard2 = newBoard1.clone();
						Line tempEnemies2 = (isFriend) ? newBoard2.getEnemies() : newBoard2.getFriends();
						
						Minion hitTwo = tempEnemies2.getAliveMinions().get(i2);
						hitTwo.takeDamage(4);
						
						int twoFirstChoices = tempEnemies2.getAliveMinions().size();
						if(twoFirstChoices == 0) {
							toRet.add(newBoard2);
						}
						
						for(int j1 = 0; j1 < twoFirstChoices; j1++) {
							Board newBoard3 = newBoard2.clone();
							Line tempEnemies3 = (isFriend) ? newBoard3.getEnemies() : newBoard3.getFriends();
							
							Minion hitThree = tempEnemies3.getAliveMinions().get(j1);
							hitThree.takeDamage(4);
							
							int twoSecondChoices = tempEnemies3.getAliveMinions().size();
							if(twoSecondChoices == 0) {
								toRet.add(newBoard3);
							}
							
							for(int j2 = 0; j2 < twoSecondChoices; j2++) {
								Board newBoard4 = newBoard3.clone();
								Line tempEnemies4 = (isFriend) ? newBoard4.getEnemies() : newBoard4.getFriends();
								
								Minion hitFour = tempEnemies4.getAliveMinions().get(j2);
								hitFour.takeDamage(4);
								
								int threeFirstChoices = tempEnemies4.getAliveMinions().size();
								if(threeFirstChoices == 0) {
									toRet.add(newBoard4);
								}
								
								for(int k1 = 0; k1 < threeFirstChoices; k1++) {
									Board newBoard5 = newBoard4.clone();
									Line tempEnemies5 = (isFriend) ? newBoard5.getEnemies() : newBoard5.getFriends();
									
									Minion hitFive = tempEnemies5.getAliveMinions().get(k1);
									hitFive.takeDamage(4);
									
									int threeSecondChoices = tempEnemies5.getAliveMinions().size();
									if(threeSecondChoices == 0) {
										toRet.add(newBoard5);
									}
									
									for(int k2 = 0; k2 < threeSecondChoices; k2++) {
										Board newBoard6 = newBoard5.clone();
										Line tempEnemies6 = (isFriend) ? newBoard6.getEnemies() : newBoard6.getFriends();
										
										Minion hitSix = tempEnemies6.getAliveMinions().get(k2);
										hitSix.takeDamage(4);
										
										toRet.add(newBoard6);
									}
								}
							}
						}
					}
				}
			}

			return toRet;
		}
		case Selfless_Hero: {
			int choices = (isFriend) ? board.getFriends().getAliveMinions().size()
					: board.getEnemies().getAliveMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();
			if (choices == 0) {
				toRet.add(board);
				return toRet;
			}

			if (friendlyRiven == 1) {
				for (int i = 0; i < choices; i++) {
					Board newBoard = board.clone();
					Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

					Minion toBuff = tempFriends.getAliveMinions().get(i);
					toBuff.setDivine(true);

					toRet.add(newBoard);
				}

				return toRet;
			} else if (friendlyRiven == 2) {
				for (int i = 0; i < choices; i++) {
					for (int j = 0; j < choices; j++) {
						Board newBoard = board.clone();
						Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

						Minion buffOne = tempFriends.getAliveMinions().get(i);
						Minion buffTwo = tempFriends.getAliveMinions().get(j);

						buffOne.setDivine(true);
						buffTwo.setDivine(true);

						toRet.add(newBoard);
					}
				}
			} else if (friendlyRiven == 3) {
				for (int i = 0; i < choices; i++) {
					for (int j = 0; j < choices; j++) {
						for (int k = 0; k < choices; k++) {
							Board newBoard = board.clone();
							Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

							Minion buffOne = tempFriends.getAliveMinions().get(i);
							Minion buffTwo = tempFriends.getAliveMinions().get(j);
							Minion buffThree = tempFriends.getAliveMinions().get(k);

							buffOne.setDivine(true);
							buffTwo.setDivine(true);
							buffThree.setDivine(true);

							toRet.add(newBoard);
						}
					}
				}
			}

			return toRet;
		}
		case Gold_Selfless_Hero: {
			int totalChoices = (isFriend) ? board.getFriends().getAliveMinions().size()
					: board.getEnemies().getAliveMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();

			if (totalChoices == 0) {
				toRet.add(board);
				return toRet;
			} else if (totalChoices == 1 || totalChoices == 2) {
				for (int i = 0; i < totalChoices; i++) {
					Board newBoard = board.clone();
					Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();
					
					Minion toBuff = tempFriends.getAliveMinions().get(i);
					toBuff.setDivine(true);
					
					toRet.add(newBoard);
				}

				return toRet;
			} else {
				for (int i = 0; i < totalChoices - 1; i++) {
					for (int j = i + 1; j < totalChoices; j++) {
						Board newBoard = board.clone();

						Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();
						Minion buffOne = tempFriends.getAliveMinions().get(i);
						Minion buffTwo = tempFriends.getAliveMinions().get(j);

						buffOne.setDivine(true);
						buffTwo.setDivine(true);

						ArrayList<Board> baronOneBoards = new ArrayList<Board>();
						if (friendlyRiven > 1) {
							for (int k = 0; k < totalChoices - 1; k++) {
								for (int l = k + 1; l < totalChoices; l++) {
									Board newerBoard = newBoard.clone();

									tempFriends = (isFriend) ? newerBoard.getFriends() : newerBoard.getEnemies();
									buffOne = tempFriends.getAliveMinions().get(k);
									buffTwo = tempFriends.getAliveMinions().get(l);

									buffOne.setDivine(true);
									buffTwo.setDivine(true);

									baronOneBoards.add(newerBoard);
								}
							}
						} else {
							toRet.add(newBoard);
						}

						ArrayList<Board> baronTwoBoards = new ArrayList<Board>();
						if (friendlyRiven > 2) {
							for (Board b : baronOneBoards) {
								for (int m = 0; m < totalChoices - 1; m++) {
									for (int n = m + 1; n < totalChoices; n++) {
										Board newestBoard = b.clone();

										tempFriends = (isFriend) ? newestBoard.getFriends() : newestBoard.getEnemies();
										buffOne = tempFriends.getAliveMinions().get(m);
										buffTwo = tempFriends.getAliveMinions().get(n);

										buffOne.setDivine(true);
										buffTwo.setDivine(true);
										
										baronTwoBoards.add(newestBoard);
									}
								}
							}

							toRet.addAll(baronTwoBoards); // golden & normal baron isn't coded yet so baron can't be >3
															// atm, so just adding these here
						} else {
							toRet.addAll(baronOneBoards);
						}
					}
				}

				return toRet;
			}
		}
		case Tortollan_Shellraiser: {
			int choices = (isFriend) ? board.getFriends().getAliveMinions().size()
					: board.getEnemies().getAliveMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();
			if (choices == 0) {
				toRet.add(board);
				return toRet;
			}

			if (friendlyRiven == 1) {
				for (int i = 0; i < choices; i++) {
					Board newBoard = board.clone();
					Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

					Minion toBuff = tempFriends.getAliveMinions().get(i);
					toBuff.setBaseAttack(toBuff.getBaseAttack() + 1);
					toBuff.setBaseHealth(toBuff.getBaseHealth() + 1);

					toRet.add(newBoard);
				}

				return toRet;
			} else if (friendlyRiven == 2) {
				for (int i = 0; i < choices; i++) {
					for (int j = 0; j < choices; j++) {
						Board newBoard = board.clone();
						Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

						Minion buffOne = tempFriends.getAliveMinions().get(i);
						Minion buffTwo = tempFriends.getAliveMinions().get(j);

						buffOne.setBaseAttack(buffOne.getBaseAttack() + 1);
						buffOne.setBaseHealth(buffOne.getBaseHealth() + 1);
						buffTwo.setBaseAttack(buffTwo.getBaseAttack() + 1);
						buffTwo.setBaseHealth(buffTwo.getBaseHealth() + 1);

						toRet.add(newBoard);
					}
				}
			} else if (friendlyRiven == 3) {
				for (int i = 0; i < choices; i++) {
					for (int j = 0; j < choices; j++) {
						for (int k = 0; k < choices; k++) {
							Board newBoard = board.clone();
							Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

							Minion buffOne = tempFriends.getAliveMinions().get(i);
							Minion buffTwo = tempFriends.getAliveMinions().get(j);
							Minion buffThree = tempFriends.getAliveMinions().get(k);

							buffOne.setBaseAttack(buffOne.getBaseAttack() + 1);
							buffOne.setBaseHealth(buffOne.getBaseHealth() + 1);
							buffTwo.setBaseAttack(buffTwo.getBaseAttack() + 1);
							buffTwo.setBaseHealth(buffTwo.getBaseHealth() + 1);
							buffThree.setBaseAttack(buffThree.getBaseAttack() + 1);
							buffThree.setBaseHealth(buffThree.getBaseHealth() + 1);

							toRet.add(newBoard);
						}
					}
				}
			}

			return toRet;
		}
		case Gold_Tortollan_Shellraiser: {
			int totalChoices = (isFriend) ? board.getFriends().getAliveMinions().size()
					: board.getEnemies().getAliveMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();

			if (totalChoices == 0) {
				toRet.add(board);
				return toRet;
			} else if (totalChoices == 1 || totalChoices == 2) {

				for (int i = 0; i < totalChoices; i++) {
					Board newBoard = board.clone();
					Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();
					
					Minion toBuff = tempFriends.getAliveMinions().get(i);
					toBuff.setBaseAttack(toBuff.getBaseAttack() + friendlyRiven); // 1 * friendlyRiven
					toBuff.setBaseHealth(toBuff.getBaseHealth() + friendlyRiven);
				}

				toRet.add(board);
				return toRet;
			} else {
				for (int i = 0; i < totalChoices - 1; i++) {
					for (int j = i + 1; j < totalChoices; j++) {
						Board newBoard = board.clone();

						Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();
						Minion buffOne = tempFriends.getAliveMinions().get(i);
						Minion buffTwo = tempFriends.getAliveMinions().get(j);

						buffOne.setBaseAttack(buffOne.getBaseAttack() + 1);
						buffOne.setBaseHealth(buffOne.getBaseHealth() + 1);

						buffTwo.setBaseAttack(buffTwo.getBaseAttack() + 1);
						buffTwo.setBaseHealth(buffTwo.getBaseHealth() + 1);

						ArrayList<Board> baronOneBoards = new ArrayList<Board>();
						if (friendlyRiven > 1) {
							for (int k = 0; k < totalChoices - 1; k++) {
								for (int l = k + 1; l < totalChoices; l++) {
									Board newerBoard = newBoard.clone();

									tempFriends = (isFriend) ? newerBoard.getFriends() : newerBoard.getEnemies();
									buffOne = tempFriends.getAliveMinions().get(k);
									buffTwo = tempFriends.getAliveMinions().get(l);

									buffOne.setBaseAttack(buffOne.getBaseAttack() + 1);
									buffOne.setBaseHealth(buffOne.getBaseHealth() + 1);

									buffTwo.setBaseAttack(buffTwo.getBaseAttack() + 1);
									buffTwo.setBaseHealth(buffTwo.getBaseHealth() + 1);

									baronOneBoards.add(newerBoard);
								}
							}
						} else {
							toRet.add(newBoard);
						}

						ArrayList<Board> baronTwoBoards = new ArrayList<Board>();
						if (friendlyRiven > 2) {
							for (Board b : baronOneBoards) {
								for (int m = 0; m < totalChoices - 1; m++) {
									for (int n = m + 1; n < totalChoices; n++) {
										Board newestBoard = b.clone();

										tempFriends = (isFriend) ? newestBoard.getFriends() : newestBoard.getEnemies();
										buffOne = tempFriends.getAliveMinions().get(m);
										buffTwo = tempFriends.getAliveMinions().get(n);

										buffOne.setBaseAttack(buffOne.getBaseAttack() + 1);
										buffOne.setBaseHealth(buffOne.getBaseHealth() + 1);

										buffTwo.setBaseAttack(buffTwo.getBaseAttack() + 1);
										buffTwo.setBaseHealth(buffTwo.getBaseHealth() + 1);

										baronTwoBoards.add(newestBoard);
									}
								}
							}

							toRet.addAll(baronTwoBoards); // golden & normal baron isn't coded yet so baron can't be >3
															// atm, so just adding these here
						} else {
							toRet.addAll(baronOneBoards);
						}
					}
				}

				return toRet;
			}
		}
		case Rat_Pack: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < rattle.getSource().getAttack(tempFriends); i++) {
				toSummon.add(new Minion(Min.Rat, false, Tribe.Beast, tempFriends, 1, 1, 1, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Rat_Pack: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < rattle.getSource().getAttack(tempFriends); i++) {
				toSummon.add(new Minion(Min.Rat, true, Tribe.Beast, tempFriends, 2, 2, 2, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Replicating_Menace: {
			Debug.log("Triggering menace", 3);
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Micro_Bot, false, Tribe.Mech, tempFriends, 1, 1, 1, 1, board, Effect.None, false,
						false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Replicating_Menace: {
			Debug.log("Triggering golden menace", 3);
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Micro_Bot, true, Tribe.Mech, tempFriends, 2, 2, 2, 1, board, Effect.None, false,
						false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Mecharoo: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Joey_Bot, false, Tribe.Mech, tempFriends, 1, 1, 1, 1, board, Effect.None, false,
					false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Mecharoo: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Joey_Bot, true, Tribe.Mech, tempFriends, 2, 2, 2, 1, board, Effect.None, false,
					false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Harvest_Golem: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Damaged_Golem, false, Tribe.Mech, tempFriends, 2, 1, 1, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Harvest_Golem: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Damaged_Golem, true, Tribe.Mech, tempFriends, 4, 2, 2, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Kindly_Grandmother: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Big_Bad_Wolf, false, Tribe.Beast, tempFriends, 3, 2, 2, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Kindly_Grandmother: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Big_Bad_Wolf, true, Tribe.Beast, tempFriends, 6, 4, 4, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Infested_Wolf: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 2; i++) {
				toSummon.add(new Minion(Min.Spider, false, Tribe.Beast, tempFriends, 1, 1, 1, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Infested_Wolf: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 2; i++) {
				toSummon.add(new Minion(Min.Spider, true, Tribe.Beast, tempFriends, 2, 2, 2, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case The_Beast: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			Line tempEnemies = (isFriend) ? board.getEnemies() : board.getFriends();

			Minion toSummon = new Minion(Min.Finkle_Einhorn, false, Tribe.None, tempEnemies, 3, 3, 3, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempEnemies.summon(toSummon, Integer.MAX_VALUE, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_The_Beast: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			Line tempEnemies = (isFriend) ? board.getEnemies() : board.getFriends();

			Minion toSummon = new Minion(Min.Finkle_Einhorn, true, Tribe.None, tempEnemies, 3, 3, 3, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempEnemies.summon(toSummon, Integer.MAX_VALUE, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Mechano_egg: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Dino_Token, false, Tribe.Mech, tempFriends, 8, 8, 8, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Mechano_egg: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Dino_Token, true, Tribe.Mech, tempFriends, 16, 16, 16, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Sated_Threshadon: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Thresh_Token, false, Tribe.Murloc, tempFriends, 1, 1, 1, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Sated_Threshadon: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Thresh_Token, true, Tribe.Murloc, tempFriends, 2, 2, 2, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case King_Bagurgle: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			for (int i = 0; i < friendlyRiven; i++) {
				for (Minion m : tempFriends.getAliveMinions()) {
					if (m.getTribe() == Tribe.Murloc || m.getTribe() == Tribe.All) {
						m.setBaseAttack(m.getBaseAttack() + (2 * friendlyRiven));
						m.setBaseHealth(m.getBaseHealth() + (2 * friendlyRiven));
					}
				}
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_King_Bagurgle: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			for (int i = 0; i < friendlyRiven; i++) {
				for (Minion m : tempFriends.getAliveMinions()) {
					if (m.getTribe() == Tribe.Murloc || m.getTribe() == Tribe.All) {
						m.setBaseAttack(m.getBaseAttack() + (4 * friendlyRiven));
						m.setBaseHealth(m.getBaseHealth() + (4 * friendlyRiven));
					}
				}
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Savannah_Highmane: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 2; i++) {
				toSummon.add(new Minion(Min.Hyena, false, Tribe.Beast, tempFriends, 2, 2, 2, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Savannah_Highmane: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 2; i++) {
				toSummon.add(new Minion(Min.Hyena, false, Tribe.Beast, tempFriends, 4, 4, 4, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Voidlord: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Voidwalker, false, Tribe.Demon, tempFriends, 1, 3, 3, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Voidlord: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Voidwalker, false, Tribe.Demon, tempFriends, 2, 6, 6, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon, rattle.getPos(), tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case None: {
			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		default:
			Debug.log("Something TERRIBLE went wrong with deathrattles.", 3);
			return null;
		}

	}

	// Legacy code: BEFORE deathrattles were implemented, this was the _attacking
	// incrementation/decrementation system
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
		for (Minion m : minions)
			if (!m.isDead(this))
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
	 * Processes all of the buffs that ANY GIVEN MINION(S) gets when a minion is
	 * summoned, and then summons the actual minion(s)
	 * 
	 * @param summonList - list of minions to be summoned
	 * @param _pos       - position to summon minions at
	 * @param from       - line that the minions are being summoned from (required
	 *                   b/c of The Beast)
	 * @param khadgar    - used to deal w/ the weird situation w/ Khadgar summoning
	 *                   copies of already buffed minions
	 * @return
	 */
	public int summon(ArrayList<Minion> summonList, int _pos, Line from, int khadgar) {
		if (khadgar == 0)
			return 0;
		if (getAliveMinions().size() >= 7 || summonList == null || summonList.size() == 0)
			return 0;

		for (Minion toSummon : summonList) {
			if (toSummon.getTribe() == Tribe.Mech || toSummon.getTribe() == Tribe.All) {
				for (Minion m : minions) {
					if (m.getEffect() == Effect.Cobalt_Guardian || m.getEffect() == Effect.Gold_Cobalt_Guardian)
						m.setDivine(true);
				}
			} else if (toSummon.getTribe() == Tribe.Beast || toSummon.getTribe() == Tribe.All) {
				for (Minion m : minions) {
					if (m.getEffect() == Effect.Pack_Leader)
						toSummon.setBaseAttack(toSummon.getBaseAttack() + 3);
					if (m.getEffect() == Effect.Gold_Pack_Leader)
						toSummon.setBaseAttack(toSummon.getBaseAttack() + 6);
					if (m.getEffect() == Effect.Mama_Bear) {
						toSummon.setBaseAttack(toSummon.getBaseAttack() + 4);
						toSummon.setBaseHealth(toSummon.getBaseHealth() + 4);
					}
					if (m.getEffect() == Effect.Gold_Mama_Bear) {
						toSummon.setBaseAttack(toSummon.getBaseAttack() + 8);
						toSummon.setBaseHealth(toSummon.getBaseHealth() + 8);
					}
				}
			}
		}

		int toRet = 0;
		for (Minion m : summonList) {
			if (minions.size() < 7) {
				minions.add(Math.min(minions.size(), _pos), m);
				toRet++;
			}

			for (int i = 1; i < khadgar; i++) {
				ArrayList<Minion> list = new ArrayList<Minion>();
				list.add(m.clone(m.getBoard(), m.getLine()));
				toRet += summon(list, _pos, from, 1);
			}
		}

		return toRet;
	}

	/**
	 * Minions getter
	 * 
	 * @return - list of minions
	 */
	public LinkedList<Minion> getMinions() {
		return this.minions;
	}

	/**
	 * Returns ONLY the minions adjacent to the given minion
	 * 
	 * @param m - minion to get minions adjacent to
	 * @return - minions adjacent to m
	 */
	public ArrayList<Minion> getAdjMinions(Minion m) {
		int _ind = this.minions.indexOf(m);
		if (_ind < 0)
			throw new NoSuchElementException();

		ArrayList<Minion> toRet = new ArrayList<Minion>();
		if (_ind == 0) {
			if (minions.size() > 1)
				toRet.add(minions.get(_ind + 1));
		} else if (_ind == minions.size() - 1) {
			if (minions.size() != 1)
				toRet.add(minions.get(minions.size() - 2));
		} else {
			toRet.add(minions.get(_ind + 1));
			toRet.add(minions.get(_ind - 1));
		}

		return toRet;
	}

	/**
	 * Returns the number of minions to be summoned according to whether there is a
	 * khadgar/gold khadgar or not
	 * 
	 * @param l - line that is doing the summoning (needed b/c of The Beast)
	 * @return - total number of minions to be summoned; [1, 3]
	 */
	private int getKhadgarMultiplier(Line l) {
		int mult = 1;
		for (Minion m : l.minions) {
			if (m.getEffect() == Effect.Khadgar && mult < 2)
				mult = 2;
			if (m.getEffect() == Effect.Gold_Khadgar && mult < 3)
				mult = 3;
		}

		return mult;
	}

	/*
	 * private void decAttacking() { _attacking = Math.max(_attacking - 1, 0); }
	 */

	private void incAttacking() {
		_attacking = (_attacking + 1) % minions.size();
	}

	/**
	 * Gets the minions that a cleave would hit
	 * 
	 * @param line - line that the minion being hit is from
	 * @param m    - minion being hit
	 * @return - all minions that would be hit by a cleave
	 */
	public ArrayList<Minion> getCleaveHits(Line line, Minion m) {
		int ind = line.minions.indexOf(m);
		ArrayList<Minion> toRet = new ArrayList<Minion>();
		toRet.add(m);

		if (ind == 0) {
			if (line.minions.size() == 1)
				return toRet;
			toRet.add(line.minions.get(ind + 1));
			return toRet;
		} else if (ind == line.minions.size() - 1) {
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
		for (Minion m : minions)
			toRet.minions.addLast(m.clone(b, toRet));

		for (Minion m : mechDeathOrder)
			toRet.mechDeathOrder.addLast(m.clone(b, toRet));

		toRet.rattles = rattles.clone(b, toRet);

		if (this.rattles.rattles != null) {
			for (int i = 0; i < this.rattles.localSize(); i++) {
				int _pos = this.minions.indexOf(this.rattles.rattles.get(i).getSource());
				toRet.rattles.rattles.get(i).setSource(toRet.minions.get(_pos));
			}
		}

		toRet._attacking = this._attacking;

		return toRet;
	}

	public ArrayList<Minion> getZappTargets() {
		int lowest = Integer.MAX_VALUE;
		ArrayList<Minion> toRet = new ArrayList<Minion>();

		for (Minion m : this.minions) {
			if (m.getAttack(this) == lowest) {
				toRet.add(m);
			} else if (m.getAttack(this) < lowest) {
				toRet = new ArrayList<Minion>();
				toRet.add(m);
			}
		}

		return toRet;
	}

	public ArrayList<Minion> getTargets() {
		boolean taunt = false;
		ArrayList<Minion> toRet = new ArrayList<Minion>();

		for (Minion m : this.minions) {
			if (m.hasTaunt()) {
				if (taunt) {
					toRet.add(m);
				} else {
					taunt = true;
					toRet = new ArrayList<Minion>();
					toRet.add(m);
				}
			} else {
				if (!taunt)
					toRet.add(m);
			}
		}

		return toRet;
	}

	public int getDamage() {
		int toRet = 0;
		for (Minion m : minions) {
			toRet += m.getStars();
		}

		// TODO: add hero tavern level
		return toRet;
	}

	public void add(int p, Minion m) {
		minions.add(p, m);
	}

	public void addLast(Minion m) {
		minions.addLast(m);
	}

	public int size() {
		return minions.size();
	}

	public boolean isEmpty() {
		return minions.isEmpty();
	}

	public String toString() {
		String toRet = "";
		if (minions.size() == 0)
			toRet += "\t<EMPTY>";
		else
			for (Minion m : minions)
				toRet += "\t" + m.toString();

		return toRet;
	}

	public void print() {
		for (Minion m : minions)
			System.out.print("\t" + m.toString());
	}

}
