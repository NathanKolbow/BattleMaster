package nkolbow.board.minions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nkolbow.debug.Debug;

public class RattleList implements Iterable<RattleEntry> {

	private LinkedList<LinkedList<RattleEntry>> depth;
	
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
	
	public void addAll(List<RattleEntry> rattles) {
		for(RattleEntry toAdd : rattles) {
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
		else if(rattle == Deathrattle.Gold_Spawn_of_NZoth || rattle == Deathrattle.Spawn_of_NZoth || rattle == Deathrattle.Goldrinn_the_Great_Wolf || rattle == Deathrattle.Gold_Goldrinn_the_Great_Wolf
				|| rattle == Deathrattle.Gold_Selfless_Hero || rattle == Deathrattle.Selfless_Hero || rattle == Deathrattle.Tortollan_Shellraiser || rattle == Deathrattle.Gold_Tortollan_Shellraiser)
			return 1;
		else if(rattle == Deathrattle.Gold_Kaboom_Bot || rattle == Deathrattle.Kaboom_Bot)
			return 2;
		return 3;
	}
	
	public RattleEntry peak() {
		if(rattles == null)
			return new RattleEntry(-1, Deathrattle.None, false);
		if(rattles.size() == 0)
			return new RattleEntry(-1, Deathrattle.Process, false);
		
		return rattles.getFirst();
	}
	
	public RattleEntry pop() {
		
	}
	
	public RattleList clone() {
		RattleList toRet = new RattleList();
		
		for(LinkedList<RattleEntry> list : depth) {
			LinkedList<RattleEntry> newList = new LinkedList<RattleEntry>();
			for(RattleEntry e : list)
				newList.add(e.clone());
			
			toRet.addAll(newList);
			toRet.addDepth();
		}
		toRet.depth.removeLast(); // b/c of the above for loop
		
		if(toRet.depth.size() != 0)
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
		return rattles.iterator();
	}
	
}

class RattleEntry {
	
	private Deathrattle rattle;
	private int pos;
	private boolean isFriend;
	
	public RattleEntry(int pos, Deathrattle rattle, boolean isFriend) {
		this.rattle = rattle;
		this.pos = pos;
		this.isFriend = isFriend;
	}
	
	public Deathrattle getRattle() { return rattle; }
	public int getPos() { return this.pos; }
	public boolean getFriend() { return this.isFriend; }
	
	public RattleEntry clone() {
		return new RattleEntry(pos, rattle, isFriend);
	}
	
	public String toString() {
		return rattle.toString();
	}
	
	public int getTier() {
		if(rattle == Deathrattle.Process || rattle == Deathrattle.None)
			return 4;
		else if(rattle == Deathrattle.Gold_Spawn_of_NZoth || rattle == Deathrattle.Spawn_of_NZoth || rattle == Deathrattle.Goldrinn_the_Great_Wolf || rattle == Deathrattle.Gold_Goldrinn_the_Great_Wolf
				|| rattle == Deathrattle.Gold_Selfless_Hero || rattle == Deathrattle.Selfless_Hero || rattle == Deathrattle.Tortollan_Shellraiser || rattle == Deathrattle.Gold_Tortollan_Shellraiser)
			return 1;
		else if(rattle == Deathrattle.Gold_Kaboom_Bot || rattle == Deathrattle.Kaboom_Bot)
			return 2;
		return 3;
	}
	
}
