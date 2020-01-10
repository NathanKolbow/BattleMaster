package nkolbow.board;

import java.util.AbstractMap.SimpleEntry;
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
	private LinkedList<SimpleEntry<Min, Boolean>> mechDeathOrder;
	private RattleList rattles; // used when deathrattles are being triggered sequentially
								// probably end up creating a special data structure for this
								// that properly orders deathrattles when they're added

	// the index of the minion next to attack
	public int _attacking = 0;

	public Line() {
		minions = new LinkedList<Minion>();
		mechDeathOrder = new LinkedList<SimpleEntry<Min, Boolean>>();
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
						Minion token = new Minion(Min.Ironhide_Token, attacker.getIsGolden(), Tribe.Beast, tempFriends,
								stat, stat, stat, 1, newBoard, Effect.None, false, false, false, false,
								new ArrayList<Deathrattle>());

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

			for (Minion m : tempFriends.minions) {
				if (m == attacker)
					continue;
				if (m.getEffect() == Effect.Festeroot_Hulk)
					m.setBaseAttack(m.getBaseAttack() + 1);
				else if (m.getEffect() == Effect.Gold_Festeroot_Hulk)
					m.setBaseAttack(m.getBaseAttack() + 2);
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

	static int ccc = 0;
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

			for (Minion m : toRemove)
				if (m.getTribe() == Tribe.Mech || m.getTribe() == Tribe.All)
					tempFriends.mechDeathOrder.add(new SimpleEntry<Min, Boolean>(m.getMinionEnum(), m.getIsGolden()));
			tempFriends.minions.removeAll(toRemove);

			// process things like JunkBot
			for (Minion alive : tempFriends.getAliveMinions()) {
				if (alive.getEffect() == Effect.Junkbot || alive.getEffect() == Effect.Gold_Junkbot) {
					int gain = (alive.getEffect() == Effect.Junkbot) ? 2 : 4;
					for (Minion dead : toRemove) {
						if (dead.getTribe() == Tribe.Mech) {
							alive.setBaseAttack(alive.getBaseAttack() + gain);
							alive.setBaseHealth(alive.getBaseHealth() + gain);
						}
					}
				} else if(alive.getEffect() == Effect.Scavenging_Hyena || alive.getEffect() == Effect.Gold_Scavenging_Hyena) {
					int gain = (alive.getEffect() == Effect.Scavenging_Hyena) ? 1 : 2;
					for(Minion dead : toRemove) {
						if(dead.getTribe() == Tribe.Beast) {
							alive.setBaseAttack(alive.getBaseAttack() + (gain * 2));
							alive.setBaseHealth(alive.getBaseHealth() + gain);
						}
					}
				}
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

			for (Minion m : toRemove)
				if (m.getTribe() == Tribe.Mech || m.getTribe() == Tribe.All)
					tempEnemies.mechDeathOrder.add(new SimpleEntry<Min, Boolean>(m.getMinionEnum(), m.getIsGolden()));
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
					if (secondChoices == 0) {
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
					if (secondChoices == 0) {
						toRet.add(newBoard1);
					}

					for (int j = 0; j < secondChoices; j++) {
						Board newBoard2 = newBoard1.clone();
						Line tempEnemies2 = (isFriend) ? newBoard2.getEnemies() : newBoard2.getFriends();

						Minion hitTwo = tempEnemies2.getAliveMinions().get(j);
						hitTwo.takeDamage(4);

						int threeChoices = tempEnemies2.getAliveMinions().size();
						if (threeChoices == 0) {
							toRet.add(newBoard2);
						}

						for (int k = 0; k < threeChoices; k++) {
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
				for (int i = 0; i < choices; i++) {
					Board newBoard = board.clone();
					Line tempEnemies = (isFriend) ? newBoard.getEnemies() : newBoard.getFriends();

					Minion hitOne = tempEnemies.getAliveMinions().get(i);
					hitOne.takeDamage(4);

					int secondChoices = tempEnemies.getAliveMinions().size();
					if (secondChoices == 0) {
						toRet.add(newBoard);
					}

					for (int j = 0; j < secondChoices; j++) {
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
					if (oneSecondChoices == 0) {
						toRet.add(newBoard1);
					}

					for (int i2 = 0; i2 < oneSecondChoices; i2++) {
						Board newBoard2 = newBoard1.clone();
						Line tempEnemies2 = (isFriend) ? newBoard2.getEnemies() : newBoard2.getFriends();

						Minion hitTwo = tempEnemies2.getAliveMinions().get(i2);
						hitTwo.takeDamage(4);

						int twoFirstChoices = tempEnemies2.getAliveMinions().size();
						if (twoFirstChoices == 0) {
							toRet.add(newBoard2);
						}

						for (int j1 = 0; j1 < twoFirstChoices; j1++) {
							Board newBoard3 = newBoard2.clone();
							Line tempEnemies3 = (isFriend) ? newBoard3.getEnemies() : newBoard3.getFriends();

							Minion hitThree = tempEnemies3.getAliveMinions().get(j1);
							hitThree.takeDamage(4);

							int twoSecondChoices = tempEnemies3.getAliveMinions().size();
							if (twoSecondChoices == 0) {
								toRet.add(newBoard3);
							}

							for (int j2 = 0; j2 < twoSecondChoices; j2++) {
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
					if (oneSecondChoices == 0) {
						toRet.add(newBoard1);
					}

					for (int i2 = 0; i2 < oneSecondChoices; i2++) {
						Board newBoard2 = newBoard1.clone();
						Line tempEnemies2 = (isFriend) ? newBoard2.getEnemies() : newBoard2.getFriends();

						Minion hitTwo = tempEnemies2.getAliveMinions().get(i2);
						hitTwo.takeDamage(4);

						int twoFirstChoices = tempEnemies2.getAliveMinions().size();
						if (twoFirstChoices == 0) {
							toRet.add(newBoard2);
						}

						for (int j1 = 0; j1 < twoFirstChoices; j1++) {
							Board newBoard3 = newBoard2.clone();
							Line tempEnemies3 = (isFriend) ? newBoard3.getEnemies() : newBoard3.getFriends();

							Minion hitThree = tempEnemies3.getAliveMinions().get(j1);
							hitThree.takeDamage(4);

							int twoSecondChoices = tempEnemies3.getAliveMinions().size();
							if (twoSecondChoices == 0) {
								toRet.add(newBoard3);
							}

							for (int j2 = 0; j2 < twoSecondChoices; j2++) {
								Board newBoard4 = newBoard3.clone();
								Line tempEnemies4 = (isFriend) ? newBoard4.getEnemies() : newBoard4.getFriends();

								Minion hitFour = tempEnemies4.getAliveMinions().get(j2);
								hitFour.takeDamage(4);

								int threeFirstChoices = tempEnemies4.getAliveMinions().size();
								if (threeFirstChoices == 0) {
									toRet.add(newBoard4);
								}

								for (int k1 = 0; k1 < threeFirstChoices; k1++) {
									Board newBoard5 = newBoard4.clone();
									Line tempEnemies5 = (isFriend) ? newBoard5.getEnemies() : newBoard5.getFriends();

									Minion hitFive = tempEnemies5.getAliveMinions().get(k1);
									hitFive.takeDamage(4);

									int threeSecondChoices = tempEnemies5.getAliveMinions().size();
									if (threeSecondChoices == 0) {
										toRet.add(newBoard5);
									}

									for (int k2 = 0; k2 < threeSecondChoices; k2++) {
										Board newBoard6 = newBoard5.clone();
										Line tempEnemies6 = (isFriend) ? newBoard6.getEnemies()
												: newBoard6.getFriends();

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
		case Ghastcoiler: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, 2 * friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.GHASTCOILER_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Gold_Ghastcoiler: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, 4 * friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.GHASTCOILER_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Sneeds_Old_Shredder: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.SNEEDS_OLD_SHREDDER_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Gold_Sneeds_Old_Shredder: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, 2 * friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.SNEEDS_OLD_SHREDDER_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Piloted_Sky_Golem: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.PILOTED_SKY_GOLEM_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Gold_Piloted_Sky_Golem: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, 2 * friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.PILOTED_SKY_GOLEM_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Piloted_Shredder: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.PILOTED_SHREDDER_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Gold_Piloted_Shredder: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, 2 * friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.PILOTED_SHREDDER_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Mounted_Raptor: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.MOUNTED_RAPTOR_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Gold_Mounted_Raptor: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			
			int openSlots = 7 - tempFriends.getAliveMinions().size();
			int summons = Math.min(openSlots, 2 * friendlyRiven);
			
			ArrayList<Board> currBoards = new ArrayList<Board>();
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			for(int i = 0; i < summons; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board b : currBoards) {
					for(Min min : Minion.MOUNTED_RAPTOR_LIST) {
						Board clone = b.clone();
						Line cloneFriends = (isFriend) ? clone.getFriends() : clone.getEnemies();
						
						cloneFriends.summon(getBaseMinion(new SimpleEntry<Min, Boolean>(min, false), cloneFriends, clone), rattle.getPos() + i, cloneFriends);
						tempBoards.add(clone);
					}
				}
			}
			
			return tempBoards;
		}
		case Demon: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			int juggleCount = 0;
			
			for(Minion m : tempFriends.getAliveMinions()) {
				if(m.getEffect() == Effect.Soul_Juggler)
					juggleCount++;
				else if(m.getEffect() == Effect.Gold_Soul_Juggler)
					juggleCount += 2;
			}
			
			ArrayList<Board> tempBoards = new ArrayList<Board>();
			ArrayList<Board> currBoards = new ArrayList<Board>();
			tempBoards.add(board);
			
			
			for(int i = 0; i < juggleCount; i++) {
				currBoards = tempBoards;
				tempBoards = new ArrayList<Board>();
				
				for(Board iBoard : currBoards) {
					int count = (isFriend) ? iBoard.getEnemies().getAliveMinions().size() : iBoard.getFriends().getAliveMinions().size();
					
					for(int j = 0; j < count; j++) {
						Board tempBoard = iBoard.clone();
						Line enemies = (isFriend) ? tempBoard.getEnemies() : tempBoard.getFriends();
						enemies.getAliveMinions().get(j).takeDamage(3);
						
						tempBoards.add(tempBoard);
					}
				}
			}
			
			return tempBoards;
		}
		case Selfless_Hero: {
			int choices = (isFriend) ? board.getFriends().getAliveNonDivineMinions().size()
					: board.getEnemies().getAliveNonDivineMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();
			if (choices == 0) {
				toRet.add(board);
				return toRet;
			}

			if (friendlyRiven == 1) {
				for (int i = 0; i < choices; i++) {
					Board newBoard = board.clone();
					Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

					Minion toBuff = tempFriends.getAliveNonDivineMinions().get(i);
					toBuff.setDivine(true);

					toRet.add(newBoard);
				}

				return toRet;
			} else if (friendlyRiven == 2) {
				for (int i = 0; i < choices; i++) {
					for (int j = 0; j < choices; j++) {
						Board newBoard = board.clone();
						Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

						Minion buffOne = tempFriends.getAliveNonDivineMinions().get(i);
						Minion buffTwo = tempFriends.getAliveNonDivineMinions().get(j);

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

							Minion buffOne = tempFriends.getAliveNonDivineMinions().get(i);
							Minion buffTwo = tempFriends.getAliveNonDivineMinions().get(j);
							Minion buffThree = tempFriends.getAliveNonDivineMinions().get(k);

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
			int totalChoices = (isFriend) ? board.getFriends().getAliveNonDivineMinions().size()
					: board.getEnemies().getAliveNonDivineMinions().size();
			ArrayList<Board> toRet = new ArrayList<Board>();

			if (totalChoices == 0) {
				toRet.add(board);
				return toRet;
			} else if (totalChoices == 1 || totalChoices == 2) {
				for (int i = 0; i < totalChoices; i++) {
					Board newBoard = board.clone();
					Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();

					Minion toBuff = tempFriends.getAliveNonDivineMinions().get(i);
					toBuff.setDivine(true);

					toRet.add(newBoard);
				}

				return toRet;
			} else {
				for (int i = 0; i < totalChoices - 1; i++) {
					for (int j = i + 1; j < totalChoices; j++) {
						Board newBoard = board.clone();

						Line tempFriends = (isFriend) ? newBoard.getFriends() : newBoard.getEnemies();
						Minion buffOne = tempFriends.getAliveNonDivineMinions().get(i);
						Minion buffTwo = tempFriends.getAliveNonDivineMinions().get(j);

						buffOne.setDivine(true);
						buffTwo.setDivine(true);

						ArrayList<Board> baronOneBoards = new ArrayList<Board>();
						if (friendlyRiven > 1) {
							for (int k = 0; k < totalChoices - 1; k++) {
								for (int l = k + 1; l < totalChoices; l++) {
									Board newerBoard = newBoard.clone();

									tempFriends = (isFriend) ? newerBoard.getFriends() : newerBoard.getEnemies();
									buffOne = tempFriends.getAliveNonDivineMinions().get(k);
									buffTwo = tempFriends.getAliveNonDivineMinions().get(l);

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
										buffOne = tempFriends.getAliveNonDivineMinions().get(m);
										buffTwo = tempFriends.getAliveNonDivineMinions().get(n);

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
				toSummon.add(new Minion(Min.Rat, false, Tribe.Beast, tempFriends, 1, 1, 1, 1, board, Effect.None, false,
						false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Rat_Pack: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < rattle.getSource().getAttack(tempFriends); i++) {
				toSummon.add(new Minion(Min.Rat, true, Tribe.Beast, tempFriends, 2, 2, 2, 1, board, Effect.None, false,
						false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Replicating_Menace: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Micro_Bot, false, Tribe.Mech, tempFriends, 1, 1, 1, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Replicating_Menace: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Micro_Bot, true, Tribe.Mech, tempFriends, 2, 2, 2, 1, board, Effect.None,
						false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Mecharoo: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Joey_Bot, false, Tribe.Mech, tempFriends, 1, 1, 1, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon.clone(board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Mecharoo: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Joey_Bot, true, Tribe.Mech, tempFriends, 2, 2, 2, 1, board, Effect.None,
					false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon.clone(board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Harvest_Golem: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Damaged_Golem, false, Tribe.Mech, tempFriends, 2, 1, 1, 1, board,
					Effect.None, false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon.clone(board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Harvest_Golem: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Damaged_Golem, true, Tribe.Mech, tempFriends, 4, 2, 2, 1, board,
					Effect.None, false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon.clone(board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Kindly_Grandmother: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Big_Bad_Wolf, false, Tribe.Beast, tempFriends, 3, 2, 2, 1, board,
					Effect.None, false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon.clone(board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Kindly_Grandmother: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Big_Bad_Wolf, true, Tribe.Beast, tempFriends, 6, 4, 4, 1, board,
					Effect.None, false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon.clone(board, tempFriends), rattle.getPos() + i, tempFriends);
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
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
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
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case The_Beast: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			Line tempEnemies = (isFriend) ? board.getEnemies() : board.getFriends();

			Minion toSummon = new Minion(Min.Finkle_Einhorn, false, Tribe.None, tempEnemies, 3, 3, 3, 1, board,
					Effect.None, false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempEnemies.summon(toSummon.clone(board, tempFriends), Integer.MAX_VALUE, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_The_Beast: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();
			Line tempEnemies = (isFriend) ? board.getEnemies() : board.getFriends();

			Minion toSummon = new Minion(Min.Finkle_Einhorn, true, Tribe.None, tempEnemies, 3, 3, 3, 1, board,
					Effect.None, false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempEnemies.summon(toSummon.clone(board, tempFriends), Integer.MAX_VALUE, tempFriends);
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
				tempFriends.summon(toSummon.clone(board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Mechano_egg: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			Minion toSummon = new Minion(Min.Dino_Token, true, Tribe.Mech, tempFriends, 16, 16, 16, 1, board,
					Effect.None, false, false, false, false, new ArrayList<Deathrattle>());

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(toSummon.clone(board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Sated_Threshadon: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Thresh_Token, false, Tribe.Murloc, tempFriends, 1, 1, 1, 1, board,
						Effect.None, false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Sated_Threshadon: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < 3; i++) {
				toSummon.add(new Minion(Min.Thresh_Token, true, Tribe.Murloc, tempFriends, 2, 2, 2, 1, board,
						Effect.None, false, false, false, false, new ArrayList<Deathrattle>()));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
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
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
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
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
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
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
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
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Kangors_Apprentice: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < Math.min(2, tempFriends.mechDeathOrder.size()); i++) {
				toSummon.add(getBaseMinion(tempFriends.mechDeathOrder.get(i), tempFriends, board));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				Debug.log("Summoning " + toSummon.get(0).getMinionEnum() + " in pos " + (rattle.getPos() + i), 2);
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
			}

			ArrayList<Board> toRet = new ArrayList<Board>();
			toRet.add(board);
			return toRet;
		}
		case Gold_Kangors_Apprentice: {
			Line tempFriends = (isFriend) ? board.getFriends() : board.getEnemies();

			ArrayList<Minion> toSummon = new ArrayList<Minion>();
			for (int i = 0; i < Math.min(4, tempFriends.mechDeathOrder.size()); i++) {
				toSummon.add(getBaseMinion(tempFriends.mechDeathOrder.get(i), tempFriends, board));
			}

			for (int i = 0; i < friendlyRiven; i++) {
				Debug.log("Summoning " + toSummon.get(0).getMinionEnum() + " in pos " + (rattle.getPos() + i), 2);
				tempFriends.summon(cloneMinionList(toSummon, board, tempFriends), rattle.getPos() + i, tempFriends);
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
			Debug.log("Apparently I haven't coded in " + rattle.getRattle() + " rattle yet.", 3);
			return null;
		}
	}

	public ArrayList<Minion> cloneMinionList(ArrayList<Minion> list, Board b, Line l) {
		ArrayList<Minion> ret = new ArrayList<Minion>();
		for (Minion m : list)
			ret.add(m.clone(b, l));
		return ret;
	}

	public Minion getBaseMinion(SimpleEntry<Min, Boolean> info, Line l, Board b) {
		switch (info.getKey()) {

		// TODO: ADD THSE
//		Curators_Amalgom,

		case Curators_Amalgom: {
			if (!info.getValue())
				return new Minion(Min.Curators_Amalgom, false, Tribe.All, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Curators_Amalgom, false, Tribe.All, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Rover_Token: {
			if (!info.getValue())
				return new Minion(Min.Rover_Token, false, Tribe.Mech, l, 2, 3, 3, 1, b, Effect.None);
			return new Minion(Min.Rover_Token, true, Tribe.Mech, l, 4, 6, 6, 1, b, Effect.None);
		}
		case Imp: {
			if (!info.getValue())
				return new Minion(Min.Imp, false, Tribe.Demon, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Imp, true, Tribe.Demon, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Rat: {
			if (!info.getValue())
				return new Minion(Min.Rat, false, Tribe.Beast, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Rat, true, Tribe.Beast, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Micro_Bot: {
			if (!info.getValue())
				return new Minion(Min.Micro_Bot, false, Tribe.Mech, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Micro_Bot, true, Tribe.Mech, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Joey_Bot: {
			if (!info.getValue())
				return new Minion(Min.Joey_Bot, false, Tribe.Mech, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Joey_Bot, true, Tribe.Mech, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Damaged_Golem: {
			if (!info.getValue())
				return new Minion(Min.Damaged_Golem, false, Tribe.Mech, l, 2, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Damaged_Golem, true, Tribe.Mech, l, 4, 2, 2, 1, b, Effect.None);
		}
		case Big_Bad_Wolf: {
			if (!info.getValue())
				return new Minion(Min.Big_Bad_Wolf, false, Tribe.Beast, l, 3, 2, 2, 1, b, Effect.None);
			return new Minion(Min.Big_Bad_Wolf, true, Tribe.Beast, l, 6, 4, 4, 1, b, Effect.None);
		}
		case Spider: {
			if (!info.getValue())
				return new Minion(Min.Spider, false, Tribe.Beast, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Spider, true, Tribe.Beast, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Finkle_Einhorn: {
			if (!info.getValue())
				return new Minion(Min.Finkle_Einhorn, false, Tribe.None, l, 3, 3, 3, 1, b, Effect.None);
			return new Minion(Min.Finkle_Einhorn, true, Tribe.None, l, 3, 3, 3, 1, b, Effect.None);
		}
		case Dino_Token: {
			if (!info.getValue())
				return new Minion(Min.Dino_Token, false, Tribe.Mech, l, 8, 8, 8, 1, b, Effect.None);
			return new Minion(Min.Dino_Token, true, Tribe.Mech, l, 16, 16, 16, 1, b, Effect.None);
		}
		case Thresh_Token: {
			if (!info.getValue())
				return new Minion(Min.Thresh_Token, false, Tribe.Murloc, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Thresh_Token, true, Tribe.Murloc, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Ironhide_Token: {
			if (!info.getValue())
				return new Minion(Min.Ironhide_Direhorn, false, Tribe.Beast, l, 5, 5, 5, 1, b, Effect.None);
			return new Minion(Min.Ironhide_Direhorn, true, Tribe.Beast, l, 10, 10, 10, 1, b, Effect.None);
		}
		case Hyena: {
			if (!info.getValue())
				return new Minion(Min.Hyena, false, Tribe.Beast, l, 2, 2, 2, 1, b, Effect.None);
			return new Minion(Min.Hyena, true, Tribe.Beast, l, 4, 4, 4, 1, b, Effect.None);
		}
		case Righteous_Protector: {
			if (!info.getValue())
				return new Minion(Min.Righteous_Protector, false, Tribe.None, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Righteous_Protector, true, Tribe.None, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Selfless_Hero: {
			if (!info.getValue())
				return new Minion(Min.Selfless_Hero, false, Tribe.None, l, 2, 1, 1, 1, b, Effect.None,
						Deathrattle.Selfless_Hero);
			return new Minion(Min.Selfless_Hero, true, Tribe.None, l, 4, 2, 2, 1, b, Effect.None,
					Deathrattle.Gold_Selfless_Hero);
		}
		case Wrath_Weaver: {
			if (!info.getValue())
				return new Minion(Min.Wrath_Weaver, false, Tribe.None, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Wrath_Weaver, true, Tribe.None, l, 2, 2, 2, 1, b, Effect.None);
		}
		case Alleycat: {
			if (!info.getValue())
				return new Minion(Min.Alleycat, false, Tribe.Beast, l, 1, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Alleycat, true, Tribe.Beast, l, 2, 2, 2, 1, b, Effect.None);

		}
		case Voidwalker: {
			if (!info.getValue())
				return new Minion(Min.Voidwalker, false, Tribe.Demon, l, 1, 3, 3, 1, b, Effect.None);
			return new Minion(Min.Voidwalker, true, Tribe.Demon, l, 2, 6, 6, 1, b, Effect.None);
		}
		case Vulgar_Homunculus: {
			if (!info.getValue())
				return new Minion(Min.Vulgar_Homunculus, false, Tribe.Demon, l, 2, 4, 4, 1, b, Effect.None);
			return new Minion(Min.Vulgar_Homunculus, true, Tribe.Demon, l, 4, 8, 8, 1, b, Effect.None);
		}
		case Mecharoo: {
			if (!info.getValue())
				return new Minion(Min.Mecharoo, false, Tribe.Mech, l, 1, 1, 1, 1, b, Effect.None, Deathrattle.Mecharoo);
			return new Minion(Min.Mecharoo, false, Tribe.Mech, l, 2, 2, 2, 1, b, Effect.None,
					Deathrattle.Gold_Mecharoo);
		}
		case Micro_Machine: {
			if (!info.getValue())
				return new Minion(Min.Micro_Machine, false, Tribe.Mech, l, 1, 2, 2, 1, b, Effect.None);
			return new Minion(Min.Micro_Machine, true, Tribe.Mech, l, 2, 4, 4, 1, b, Effect.None);
		}
		case Murloc_Tidecaller: {
			if (!info.getValue())
				return new Minion(Min.Murloc_Tidecaller, false, Tribe.Murloc, l, 1, 2, 2, 1, b, Effect.None);
			return new Minion(Min.Murloc_Tidecaller, true, Tribe.Murloc, l, 2, 4, 4, 1, b, Effect.None);
		}
		case Murloc_Tidehunter: {
			if (!info.getValue())
				return new Minion(Min.Murloc_Tidehunter, false, Tribe.Murloc, l, 2, 1, 1, 1, b, Effect.None);
			return new Minion(Min.Murloc_Tidehunter, true, Tribe.Murloc, l, 4, 2, 2, 1, b, Effect.None);
		}
		case Rockpool_Hunter: {
			if (!info.getValue())
				return new Minion(Min.Rockpool_Hunter, false, Tribe.Murloc, l, 2, 3, 3, 1, b, Effect.None);
			return new Minion(Min.Rockpool_Hunter, true, Tribe.Murloc, l, 4, 6, 6, 1, b, Effect.None);
		}
		case Dire_Wolf_Alpha: {
			if (!info.getValue())
				return new Minion(Min.Dire_Wolf_Alpha, false, Tribe.Beast, l, 2, 2, 2, 2, b, Effect.Dire_Wolf_Alpha);
			return new Minion(Min.Dire_Wolf_Alpha, true, Tribe.Beast, l, 4, 4, 4, 2, b, Effect.Gold_Dire_Wolf_Alpha);
		}
		case Spawn_of_NZoth: {
			if (!info.getValue())
				return new Minion(Min.Spawn_of_NZoth, false, Tribe.None, l, 2, 2, 2, 2, b, Effect.None,
						Deathrattle.Spawn_of_NZoth);
			return new Minion(Min.Spawn_of_NZoth, false, Tribe.None, l, 4, 4, 4, 2, b, Effect.None,
					Deathrattle.Gold_Spawn_of_NZoth);
		}
		case Kindly_Grandmother: {
			if (!info.getValue())
				return new Minion(Min.Kindly_Grandmother, false, Tribe.None, l, 1, 1, 1, 2, b, Effect.None,
						Deathrattle.Kindly_Grandmother);
			return new Minion(Min.Kindly_Grandmother, true, Tribe.None, l, 2, 2, 2, 2, b, Effect.None,
					Deathrattle.Gold_Kindly_Grandmother);
		}
		case Mounted_Raptor: {
			if (!info.getValue())
				return new Minion(Min.Mounted_Raptor, false, Tribe.Beast, l, 3, 2, 2, 2, b, Effect.None,
						Deathrattle.Mounted_Raptor);
			return new Minion(Min.Mounted_Raptor, true, Tribe.Beast, l, 6, 4, 4, 2, b, Effect.None,
					Deathrattle.Gold_Mounted_Raptor);
		}
		case Rat_Pack: {
			if (!info.getValue())
				return new Minion(Min.Rat_Pack, false, Tribe.Beast, l, 2, 2, 2, 2, b, Effect.None,
						Deathrattle.Rat_Pack);
			return new Minion(Min.Rat_Pack, true, Tribe.Beast, l, 4, 4, 4, 2, b, Effect.None,
					Deathrattle.Gold_Rat_Pack);
		}
		case Scavenging_Hyena: {
			if (!info.getValue())
				return new Minion(Min.Scavenging_Hyena, false, Tribe.Beast, l, 2, 2, 2, 2, b, Effect.Scavenging_Hyena);
			return new Minion(Min.Scavenging_Hyena, true, Tribe.Beast, l, 4, 4, 4, 2, b, Effect.Gold_Scavenging_Hyena);
		}
		case Nathrezim_Overseer: {
			if (!info.getValue())
				return new Minion(Min.Nathrezim_Overseer, false, Tribe.Demon, l, 2, 4, 4, 2, b, Effect.None);
			return new Minion(Min.Nathrezim_Overseer, true, Tribe.Demon, l, 4, 8, 8, 2, b, Effect.None);
		}
		case Annoy_o_Tron: {
			if (!info.getValue())
				return new Minion(Min.Annoy_o_Tron, false, Tribe.Mech, l, 1, 2, 2, 2, b, Effect.None, true, false,
						false, true);
			return new Minion(Min.Annoy_o_Tron, true, Tribe.Mech, l, 2, 4, 4, 2, b, Effect.None, true, false, false,
					true);
		}
		case Harvest_Golem: {
			if (!info.getValue())
				return new Minion(Min.Harvest_Golem, false, Tribe.Mech, l, 2, 1, 1, 2, b, Effect.None,
						Deathrattle.Harvest_Golem);
			return new Minion(Min.Harvest_Golem, true, Tribe.Mech, l, 4, 2, 2, 2, b, Effect.None,
					Deathrattle.Gold_Harvest_Golem);
		}
		case Kaboom_Bot: {
			if (!info.getValue())
				return new Minion(Min.Kaboom_Bot, false, Tribe.Mech, l, 2, 2, 2, 2, b, Effect.None,
						Deathrattle.Kaboom_Bot);
			return new Minion(Min.Kaboom_Bot, true, Tribe.Mech, l, 4, 4, 4, 2, b, Effect.None,
					Deathrattle.Gold_Kaboom_Bot);
		}
		case Metaltooth_Leaper: {
			if (!info.getValue())
				return new Minion(Min.Metaltooth_Leaper, false, Tribe.Mech, l, 3, 3, 3, 2, b, Effect.None);
			return new Minion(Min.Metaltooth_Leaper, true, Tribe.Mech, l, 6, 6, 6, 2, b, Effect.None);
		}
		case Pogo_Hopper: {
			if (!info.getValue())
				return new Minion(Min.Pogo_Hopper, false, Tribe.Mech, l, 1, 1, 1, 2, b, Effect.None);
			return new Minion(Min.Pogo_Hopper, true, Tribe.Mech, l, 2, 2, 2, 2, b, Effect.None);
		}
		case Shielded_Minibot: {
			if (!info.getValue())
				return new Minion(Min.Shielded_Minibot, false, Tribe.Mech, l, 2, 2, 2, 2, b, Effect.None, true, false,
						false, false);
			return new Minion(Min.Shielded_Minibot, true, Tribe.Mech, l, 4, 4, 4, 2, b, Effect.None, true, false, false,
					false);
		}
		case Zoobot: {
			if (!info.getValue())
				return new Minion(Min.Zoobot, false, Tribe.Mech, l, 3, 3, 3, 2, b, Effect.None);
			return new Minion(Min.Zoobot, true, Tribe.Mech, l, 6, 6, 6, 2, b, Effect.None);
		}
		case Coldlight_Seer: {
			if (!info.getValue())
				return new Minion(Min.Coldlight_Seer, false, Tribe.Murloc, l, 2, 3, 3, 2, b, Effect.None);
			return new Minion(Min.Coldlight_Seer, true, Tribe.Murloc, l, 4, 6, 6, 2, b, Effect.None);
		}
		case Old_Murk_Eye: {
			if (!info.getValue())
				return new Minion(Min.Old_Murk_Eye, false, Tribe.Murloc, l, 2, 4, 4, 2, b, Effect.Old_Murk_Eye);
			return new Minion(Min.Old_Murk_Eye, true, Tribe.Murloc, l, 4, 8, 8, 2, b, Effect.Gold_Old_Murk_Eye);
		}
		case Murloc_Warleader: {
			if (!info.getValue())
				return new Minion(Min.Murloc_Warleader, false, Tribe.Murloc, l, 3, 3, 3, 2, b, Effect.Murloc_Warleader);
			return new Minion(Min.Murloc_Warleader, true, Tribe.Murloc, l, 6, 6, 6, 2, b, Effect.Gold_Murloc_Warleader);
		}
		case Nightmare_Amalgam: {
			if (!info.getValue())
				return new Minion(Min.Nightmare_Amalgam, false, Tribe.All, l, 3, 4, 4, 3, b, Effect.None);
			return new Minion(Min.Nightmare_Amalgam, true, Tribe.All, l, 6, 8, 8, 3, b, Effect.None);
		}
		case Crowd_Favorite: {
			if (!info.getValue())
				return new Minion(Min.Crowd_Favorite, false, Tribe.None, l, 4, 4, 4, 3, b, Effect.None);
			return new Minion(Min.Crowd_Favorite, true, Tribe.None, l, 8, 8, 8, 3, b, Effect.None);
		}
		case Crystalweaver: {
			if (!info.getValue())
				return new Minion(Min.Crystalweaver, false, Tribe.None, l, 5, 4, 4, 3, b, Effect.None);
			return new Minion(Min.Crystalweaver, true, Tribe.None, l, 10, 8, 8, 3, b, Effect.None);
		}
		case Houndmaster: {
			if (!info.getValue())
				return new Minion(Min.Houndmaster, false, Tribe.None, l, 4, 3, 3, 3, b, Effect.None);
			return new Minion(Min.Houndmaster, true, Tribe.None, l, 8, 6, 6, 3, b, Effect.None);
		}
		case Shifter_Zerus: {
			if (!info.getValue())
				return new Minion(Min.Shifter_Zerus, false, Tribe.None, l, 1, 1, 1, 3, b, Effect.None);
			return new Minion(Min.Shifter_Zerus, true, Tribe.None, l, 2, 2, 2, 3, b, Effect.None);
		}
		case Tortollan_Shellraiser: {
			if (!info.getValue())
				return new Minion(Min.Tortollan_Shellraiser, false, Tribe.None, l, 2, 6, 6, 3, b, Effect.None,
						Deathrattle.Tortollan_Shellraiser);
			return new Minion(Min.Tortollan_Shellraiser, true, Tribe.None, l, 4, 12, 12, 3, b, Effect.None,
					Deathrattle.Gold_Tortollan_Shellraiser);
		}
		case Infested_Wolf: {
			if (!info.getValue())
				return new Minion(Min.Infested_Wolf, false, Tribe.Beast, l, 3, 3, 3, 3, b, Effect.None,
						Deathrattle.Infested_Wolf);
			return new Minion(Min.Infested_Wolf, true, Tribe.Beast, l, 6, 6, 6, 3, b, Effect.None,
					Deathrattle.Gold_Infested_Wolf);
		}
		case Imp_Gang_Boss: {
			if (!info.getValue())
				return new Minion(Min.Imp_Gang_Boss, false, Tribe.Demon, l, 2, 4, 4, 3, b, Effect.Imp_Gang_Boss);
			return new Minion(Min.Imp_Gang_Boss, true, Tribe.Demon, l, 4, 8, 8, 3, b, Effect.Gold_Imp_Gang_Boss);
		}
		case Floating_Watcher: {
			if (!info.getValue())
				return new Minion(Min.Floating_Watcher, false, Tribe.Demon, l, 4, 4, 4, 3, b, Effect.None);
			return new Minion(Min.Floating_Watcher, true, Tribe.Demon, l, 8, 8, 8, 3, b, Effect.None);
		}
		case Cobalt_Guardian: {
			if (!info.getValue())
				return new Minion(Min.Cobalt_Guardian, false, Tribe.Mech, l, 6, 3, 3, 3, b, Effect.Cobalt_Guardian);
			return new Minion(Min.Cobalt_Guardian, true, Tribe.Mech, l, 12, 6, 6, 3, b, Effect.Gold_Cobalt_Guardian);
		}
		case Piloted_Shredder: {
			if (!info.getValue())
				return new Minion(Min.Piloted_Shredder, false, Tribe.Mech, l, 4, 3, 3, 3, b, Effect.None,
						Deathrattle.Piloted_Shredder);
			return new Minion(Min.Piloted_Shredder, true, Tribe.Mech, l, 8, 6, 6, 3, b, Effect.None,
					Deathrattle.Gold_Piloted_Shredder);
		}
		case Psych_o_Tron: {
			if (!info.getValue())
				return new Minion(Min.Psych_o_Tron, false, Tribe.Mech, l, 3, 4, 4, 3, b, Effect.None, true, false,
						false, true);
			return new Minion(Min.Psych_o_Tron, true, Tribe.Mech, l, 6, 8, 8, 3, b, Effect.None, true, false, false,
					true);
		}
		case Replicating_Menace: {
			if (!info.getValue())
				return new Minion(Min.Replicating_Menace, false, Tribe.Mech, l, 3, 1, 1, 3, b, Effect.None,
						Deathrattle.Replicating_Menace);
			return new Minion(Min.Replicating_Menace, true, Tribe.Mech, l, 6, 2, 2, 3, b, Effect.None,
					Deathrattle.Gold_Replicating_Menace);
		}
		case Screwjank_Clunker: {
			if (!info.getValue())
				return new Minion(Min.Screwjank_Clunker, false, Tribe.Mech, l, 2, 5, 5, 3, b, Effect.None);
			return new Minion(Min.Screwjank_Clunker, true, Tribe.Mech, l, 4, 10, 10, 3, b, Effect.None);
		}
		case Khadgar: {
			if (!info.getValue())
				return new Minion(Min.Khadgar, false, Tribe.None, l, 2, 2, 2, 3, b, Effect.Khadgar);
			return new Minion(Min.Khadgar, true, Tribe.None, l, 4, 4, 4, 3, b, Effect.Gold_Khadgar);
		}
		case Pack_Leader: {
			if (!info.getValue())
				return new Minion(Min.Pack_Leader, false, Tribe.None, l, 3, 3, 3, 3, b, Effect.Pack_Leader);
			return new Minion(Min.Pack_Leader, true, Tribe.None, l, 6, 6, 6, 3, b, Effect.Gold_Pack_Leader);
		}
		case Phalanx_Commander: {
			if (!info.getValue())
				return new Minion(Min.Phalanx_Commander, false, Tribe.None, l, 4, 5, 5, 3, b, Effect.Phalanx_Commander);
			return new Minion(Min.Phalanx_Commander, true, Tribe.None, l, 8, 10, 10, 3, b,
					Effect.Gold_Phalanx_Commander);
		}
		case Soul_Juggler: {
			if (!info.getValue())
				return new Minion(Min.Soul_Juggler, false, Tribe.None, l, 3, 3, 3, 3, b, Effect.Soul_Juggler);
			return new Minion(Min.Soul_Juggler, true, Tribe.None, l, 6, 6, 6, 3, b, Effect.Gold_Soul_Juggler);
		}
		case Defender_of_Argus: {
			if (!info.getValue())
				return new Minion(Min.Defender_of_Argus, false, Tribe.None, l, 2, 3, 3, 4, b, Effect.None);
			return new Minion(Min.Defender_of_Argus, true, Tribe.None, l, 4, 6, 6, 4, b, Effect.None);
		}
		case Menagerie_Magician: {
			if (!info.getValue())
				return new Minion(Min.Menagerie_Magician, false, Tribe.None, l, 4, 4, 4, 4, b, Effect.None);
			return new Minion(Min.Menagerie_Magician, true, Tribe.None, l, 8, 8, 8, 4, b, Effect.None);
		}
		case Virmen_Sensei: {
			if (!info.getValue())
				return new Minion(Min.Virmen_Sensei, false, Tribe.None, l, 4, 5, 5, 4, b, Effect.None);
			return new Minion(Min.Virmen_Sensei, true, Tribe.None, l, 8, 10, 10, 4, b, Effect.None);
		}
		case Bolvar_Fireblood: {
			if (!info.getValue())
				return new Minion(Min.Bolvar_Fireblood, false, Tribe.None, l, 1, 7, 7, 4, b, Effect.Bolvar_Fireblood,
						true, false, false, false);
			return new Minion(Min.Bolvar_Fireblood, true, Tribe.None, l, 2, 14, 14, 4, b, Effect.Gold_Bolvar_Fireblood,
					true, false, false, false);
		}
		case Festeroot_Hulk: {
			if (!info.getValue())
				return new Minion(Min.Festeroot_Hulk, false, Tribe.None, l, 2, 7, 7, 4, b, Effect.Festeroot_Hulk);
			return new Minion(Min.Festeroot_Hulk, true, Tribe.None, l, 4, 14, 14, 4, b, Effect.Festeroot_Hulk);
		}
		case Cave_Hydra: {
			if (!info.getValue())
				return new Minion(Min.Cave_Hydra, false, Tribe.Beast, l, 2, 4, 4, 4, b, Effect.None, false, false, true,
						false);
			return new Minion(Min.Cave_Hydra, true, Tribe.Beast, l, 4, 8, 8, 4, b, Effect.None, false, false, true,
					false);
		}
		case The_Beast: {
			if (!info.getValue())
				return new Minion(Min.The_Beast, false, Tribe.Beast, l, 9, 7, 7, 4, b, Effect.None,
						Deathrattle.The_Beast);
			return new Minion(Min.The_Beast, true, Tribe.Beast, l, 18, 14, 14, 4, b, Effect.None,
					Deathrattle.Gold_The_Beast);
		}
		case Siegebreaker: {
			if (!info.getValue())
				return new Minion(Min.Siegebreaker, false, Tribe.Demon, l, 5, 8, 8, 4, b, Effect.Siegebreaker);
			return new Minion(Min.Siegebreaker, true, Tribe.Demon, l, 10, 16, 16, 4, b, Effect.Gold_Siegebreaker);
		}
		case Annoy_o_Module: {
			if (!info.getValue())
				return new Minion(Min.Annoy_o_Module, false, Tribe.Mech, l, 2, 4, 4, 4, b, Effect.None);
			return new Minion(Min.Annoy_o_Module, true, Tribe.Mech, l, 4, 8, 8, 4, b, Effect.None);
		}
		case Iron_Sensei: {
			if (!info.getValue())
				return new Minion(Min.Iron_Sensei, false, Tribe.Mech, l, 2, 2, 2, 4, b, Effect.None);
			return new Minion(Min.Iron_Sensei, true, Tribe.Mech, l, 4, 4, 4, 4, b, Effect.None);
		}
		case Piloted_Sky_Golem: {
			if (!info.getValue())
				return new Minion(Min.Piloted_Sky_Golem, false, Tribe.Mech, l, 6, 4, 4, 4, b, Effect.None);
			return new Minion(Min.Piloted_Sky_Golem, true, Tribe.Mech, l, 12, 8, 8, 4, b, Effect.None);
		}
		case Security_Rover: {
			if (!info.getValue())
				return new Minion(Min.Security_Rover, false, Tribe.Mech, l, 2, 6, 6, 4, b, Effect.Security_Rover);
			return new Minion(Min.Security_Rover, true, Tribe.Mech, l, 4, 12, 12, 4, b, Effect.Gold_Security_Rover);
		}
		case Toxfin: {
			if (!info.getValue())
				return new Minion(Min.Toxfin, false, Tribe.Murloc, l, 1, 2, 2, 4, b, Effect.None);
			return new Minion(Min.Toxfin, true, Tribe.Murloc, l, 2, 4, 4, 4, b, Effect.None);
		}
		case Brann_Bronzebeard: {
			if (!info.getValue())
				return new Minion(Min.Brann_Bronzebeard, false, Tribe.None, l, 2, 4, 4, 4, b, Effect.None);
			return new Minion(Min.Brann_Bronzebeard, true, Tribe.None, l, 4, 8, 8, 4, b, Effect.None);
		}
		case Lightfang_Enforcer: {
			if (!info.getValue())
				return new Minion(Min.Lightfang_Enforcer, false, Tribe.None, l, 2, 2, 2, 5, b, Effect.None);
			return new Minion(Min.Lightfang_Enforcer, true, Tribe.None, l, 4, 4, 4, 5, b, Effect.None);
		}
		case Strongshell_Scavenger: {
			if (!info.getValue())
				return new Minion(Min.Strongshell_Scavenger, false, Tribe.None, l, 2, 3, 3, 5, b, Effect.None);
			return new Minion(Min.Strongshell_Scavenger, true, Tribe.None, l, 4, 6, 6, 5, b, Effect.None);
		}
		case Baron_Rivendare: {
			if (!info.getValue())
				return new Minion(Min.Baron_Rivendare, false, Tribe.None, l, 1, 7, 7, 5, b, Effect.Baron_Rivendare);
			return new Minion(Min.Baron_Rivendare, true, Tribe.None, l, 2, 14, 14, 5, b, Effect.Gold_Baron_Rivendare);
		}
		case Goldrinn_the_Great_Wolf: {
			if (!info.getValue())
				return new Minion(Min.Goldrinn_the_Great_Wolf, false, Tribe.Beast, l, 4, 4, 4, 5, b, Effect.None,
						Deathrattle.Goldrinn_the_Great_Wolf);
			return new Minion(Min.Goldrinn_the_Great_Wolf, true, Tribe.Beast, l, 8, 8, 8, 5, b, Effect.None,
					Deathrattle.Gold_Goldrinn_the_Great_Wolf);
		}
		case Ironhide_Direhorn: {
			if (!info.getValue())
				return new Minion(Min.Ironhide_Direhorn, false, Tribe.Beast, l, 7, 7, 7, 5, b,
						Effect.Ironhide_Direhorn);
			return new Minion(Min.Ironhide_Direhorn, true, Tribe.Beast, l, 14, 14, 14, 5, b,
					Effect.Gold_Ironhide_Direhorn);
		}
		case Sated_Threshadon: {
			if (!info.getValue())
				return new Minion(Min.Sated_Threshadon, false, Tribe.Beast, l, 5, 7, 7, 5, b, Effect.None,
						Deathrattle.Sated_Threshadon);
			return new Minion(Min.Sated_Threshadon, true, Tribe.Beast, l, 10, 14, 14, 5, b, Effect.None,
					Deathrattle.Gold_Sated_Threshadon);
		}
		case Savannah_Highmane: {
			if (!info.getValue())
				return new Minion(Min.Savannah_Highmane, false, Tribe.Beast, l, 6, 5, 5, 5, b, Effect.None,
						Deathrattle.Savannah_Highmane);
			return new Minion(Min.Savannah_Highmane, true, Tribe.Beast, l, 12, 10, 10, 5, b, Effect.None,
					Deathrattle.Gold_Savannah_Highmane);
		}
		case Annihilan_Battlemaster: {
			if (!info.getValue())
				return new Minion(Min.Annihilan_Battlemaster, false, Tribe.Demon, l, 3, 1, 1, 5, b, Effect.None);
			return new Minion(Min.Annihilan_Battlemaster, true, Tribe.Demon, l, 6, 2, 2, 5, b, Effect.None);
		}
		case MalGanis: {
			if (!info.getValue())
				return new Minion(Min.MalGanis, false, Tribe.Demon, l, 9, 7, 7, 5, b, Effect.MalGanis);
			return new Minion(Min.MalGanis, true, Tribe.Demon, l, 18, 14, 14, 5, b, Effect.Gold_MalGanis);
		}
		case Voidlord: {
			if (!info.getValue())
				return new Minion(Min.Voidlord, false, Tribe.Demon, l, 3, 9, 9, 5, b, Effect.None,
						Deathrattle.Voidlord);
			return new Minion(Min.Voidlord, true, Tribe.Demon, l, 6, 18, 18, 5, b, Effect.None,
					Deathrattle.Gold_Voidlord);
		}
		case Junkbot: {
			if (!info.getValue())
				return new Minion(Min.Junkbot, false, Tribe.Mech, l, 1, 5, 5, 5, b, Effect.Junkbot);
			return new Minion(Min.Junkbot, true, Tribe.Mech, l, 2, 10, 10, 5, b, Effect.Gold_Junkbot);
		}
		case Mechano_Egg: {
			if (!info.getValue())
				return new Minion(Min.Mechano_Egg, false, Tribe.Mech, l, 0, 5, 5, 5, b, Effect.None,
						Deathrattle.Mechano_egg);
			return new Minion(Min.Mechano_Egg, true, Tribe.Mech, l, 0, 10, 10, 5, b, Effect.None,
					Deathrattle.Gold_Mechano_egg);
		}
		case King_Bagurgle: {
			if (!info.getValue())
				return new Minion(Min.King_Bagurgle, false, Tribe.Murloc, l, 6, 3, 3, 5, b, Effect.None,
						Deathrattle.King_Bagurgle);
			return new Minion(Min.King_Bagurgle, true, Tribe.Murloc, l, 12, 6, 6, 5, b, Effect.None,
					Deathrattle.Gold_King_Bagurgle);
		}
		case Primalfin_Lookout: {
			if (!info.getValue())
				return new Minion(Min.Primalfin_Lookout, false, Tribe.Murloc, l, 3, 2, 2, 5, b, Effect.None);
			return new Minion(Min.Primalfin_Lookout, true, Tribe.Murloc, l, 6, 4, 4, 5, b, Effect.None);
		}
		case Kangors_Apprentice: {
			if (!info.getValue())
				return new Minion(Min.Kangors_Apprentice, false, Tribe.None, l, 3, 6, 6, 6, b, Effect.None,
						Deathrattle.Kangors_Apprentice);
			return new Minion(Min.Kangors_Apprentice, true, Tribe.None, l, 6, 12, 12, 6, b, Effect.None,
					Deathrattle.Gold_Kangors_Apprentice);
		}
		case Zapp_Slywick: {
			if (!info.getValue())
				return new Minion(Min.Zapp_Slywick, false, Tribe.None, l, 7, 10, 10, 6, b, Effect.Zapp_Slywick);
			return new Minion(Min.Zapp_Slywick, true, Tribe.None, l, 14, 20, 20, 6, b, Effect.Gold_Zapp_Slywick);
		}
		case Gentle_Megasaur: {
			if (!info.getValue())
				return new Minion(Min.Gentle_Megasaur, false, Tribe.Beast, l, 5, 4, 4, 6, b, Effect.None);
			return new Minion(Min.Gentle_Megasaur, true, Tribe.Beast, l, 10, 8, 8, 6, b, Effect.None);
		}
		case Ghastcoiler: {
			if (!info.getValue())
				return new Minion(Min.Ghastcoiler, false, Tribe.Beast, l, 7, 7, 7, 6, b, Effect.None,
						Deathrattle.Ghastcoiler);
			return new Minion(Min.Ghastcoiler, true, Tribe.Beast, l, 14, 14, 14, 6, b, Effect.None,
					Deathrattle.Gold_Ghastcoiler);
		}
		case Maexxna: {
			if (!info.getValue())
				return new Minion(Min.Maexxna, false, Tribe.Beast, l, 2, 8, 8, 6, b, Effect.None, false, true, false,
						false);
			return new Minion(Min.Maexxna, true, Tribe.Beast, l, 4, 16, 16, 6, b, Effect.None, false, true, false,
					false);
		}
		case Mama_Bear: {
			if (!info.getValue())
				return new Minion(Min.Mama_Bear, false, Tribe.Beast, l, 4, 4, 4, 6, b, Effect.Mama_Bear);
			return new Minion(Min.Mama_Bear, true, Tribe.Beast, l, 8, 8, 8, 6, b, Effect.Gold_Mama_Bear);
		}
		case Foe_Reaper_4000: {
			if (!info.getValue())
				return new Minion(Min.Foe_Reaper_4000, false, Tribe.Mech, l, 6, 9, 9, 6, b, Effect.None, false, false,
						true, false);
			return new Minion(Min.Foe_Reaper_4000, true, Tribe.Mech, l, 12, 18, 18, 6, b, Effect.None, false, false,
					true, false);
		}
		case Sneeds_Old_Shredder: {
			if (!info.getValue())
				return new Minion(Min.Sneeds_Old_Shredder, false, Tribe.Mech, l, 5, 7, 7, 6, b, Effect.None,
						Deathrattle.Sneeds_Old_Shredder);
			return new Minion(Min.Sneeds_Old_Shredder, true, Tribe.Mech, l, 10, 14, 14, 6, b, Effect.None,
					Deathrattle.Gold_Sneeds_Old_Shredder);
		}
		default:
			Debug.log("This shouldn't be possible, must've missed a minion.", 3);
			break;
		}

		return null;
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

	public ArrayList<Minion> getAliveNonDivineMinions() {
		ArrayList<Minion> toRet = new ArrayList<Minion>();
		for(Minion m : minions)
			if(!m.isDead(this) && !m.hasDivine())
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
					if (m.getEffect() == Effect.Cobalt_Guardian || m.getEffect() == Effect.Gold_Cobalt_Guardian) {
						m.setDivine(true);
					}
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
				minions.add(Math.min(minions.size(), _pos + toRet), m);
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

		for (SimpleEntry<Min, Boolean> e : mechDeathOrder)
			toRet.mechDeathOrder.addLast(new SimpleEntry<Min, Boolean>(e.getKey(), e.getValue()));

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
