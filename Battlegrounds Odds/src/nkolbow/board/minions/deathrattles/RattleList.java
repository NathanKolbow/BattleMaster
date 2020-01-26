package nkolbow.board.minions.deathrattles;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nkolbow.board.Board;
import nkolbow.board.Line;
import nkolbow.board.minions.Minion;

public class RattleList implements Iterable<RattleEntry> {

	public LinkedList<LinkedList<RattleEntry>> depth;
	
	public LinkedList<RattleEntry> rattles;
	
	public RattleList() {
		rattles = new LinkedList<RattleEntry>();
		depth = new LinkedList<LinkedList<RattleEntry>>();
		
		depth.add(rattles);
	}
	
	public boolean isEmpty() {
		return depth.size() == 1 && rattles.size() == 0;
	}
	
	public void addDepth() {
		depth.add(new LinkedList<RattleEntry>());
	}
	
	public void addAll(int _pos, List<Deathrattle> rattles, Minion source) {
		for(Deathrattle r : rattles)
			add(new RattleEntry(_pos, r, source));
	}
	
	public void addAll(List<RattleEntry> rattles) {
		for(RattleEntry toAdd : rattles) {
			if(toAdd.getRattle() != Deathrattle.None && toAdd.getRattle() != Deathrattle.Process)
				add(toAdd);
		}
	}
	
	public void add(RattleEntry rattle) {
		if(depth.size() == 0)
			addDepth();
		
		LinkedList<RattleEntry> addTo = depth.getLast();
		int tier = getTier(rattle.getRattle());
		
		int i = 0;
		for(; i < addTo.size(); i++) {
			if(getTier(addTo.get(i).getRattle()) > tier)
				break;
		}
		
		addTo.add(i, rattle);
	}
	
	/*
	 * ORDER OF DEATHRATTLES:
	 * 		1.  Spawn of N'Zoth
	 * 			Goldrinn, the Great Wolf
	 * 			Selfless Hero
	 * 			Tortollan Shellraiser
	 * 		2.	Kaboom Bot
	 * 		3.	Everything else (order is arbitrary, only summoning happens)
	 */
	
	public int getTier(Deathrattle rattle) {
		if(rattle == Deathrattle.Process || rattle == Deathrattle.None)
			return 4;
		else if(rattle == Deathrattle.Reborn || rattle == Deathrattle.Gold_Spawn_of_NZoth || rattle == Deathrattle.Spawn_of_NZoth || rattle == Deathrattle.Goldrinn_the_Great_Wolf || rattle == Deathrattle.Gold_Goldrinn_the_Great_Wolf
				|| rattle == Deathrattle.Gold_Selfless_Hero || rattle == Deathrattle.Selfless_Hero || rattle == Deathrattle.Tortollan_Shellraiser || rattle == Deathrattle.Gold_Tortollan_Shellraiser)
			return 1;
		else if(rattle == Deathrattle.Gold_Kaboom_Bot || rattle == Deathrattle.Kaboom_Bot)
			return 2;
		return 3;
	}
	
	public RattleEntry peak() {
		if(rattles == null)
			return new RattleEntry(-1, Deathrattle.None, null);
		if(rattles.size() == 0)
			return new RattleEntry(-1, Deathrattle.Process, null);
		
		return rattles.getFirst();
	}
	
	public RattleEntry pop() {
		if(rattles == null)
			return new RattleEntry(-1, Deathrattle.None, null);
		if(rattles.size() == 0) {
			if(depth.size() <= 1) {
				rattles = null;
				depth = new LinkedList<LinkedList<RattleEntry>>();
			} else {
				depth.removeFirst();
				rattles = depth.get(0);
			}
			
			return new RattleEntry(-1, Deathrattle.Process, null);
		} else {
			return rattles.removeFirst();
		}
	}
	
	public int localSize() {
		if(rattles == null) {
			return 0;
		} else {
			return rattles.size();
		}
	}
	
	public RattleList clone(Board b, Line l) {
		RattleList toRet = new RattleList();
		
		LinkedList<LinkedList<RattleEntry>> newDepth = new LinkedList<LinkedList<RattleEntry>>();
		for(int i = 0; i < depth.size(); i++) {
			newDepth.add(new LinkedList<RattleEntry>());
			for(RattleEntry e : depth.get(i)) {
				newDepth.get(i).add(e.clone(b, l));
			}
		}
		toRet.depth = newDepth;
		if(toRet.depth.size() == 0)
			toRet.rattles = null;
		else
			toRet.rattles = toRet.depth.get(0);
		
		return toRet;
	}
	
	@Override
	public String toString() {
		String toRet = "";
		
		for(LinkedList<RattleEntry> list : depth) {
			for(RattleEntry rattle : list) {
				toRet += rattle.getRattle().toString() + ", ";
			}
			toRet = toRet.substring(0, toRet.length() - 2);
			toRet += "\n";
		} 
		
		return toRet;
	}
	
	@Override
	public Iterator<RattleEntry> iterator() {
		return (rattles == null) ? null : rattles.iterator();
	}
	
}